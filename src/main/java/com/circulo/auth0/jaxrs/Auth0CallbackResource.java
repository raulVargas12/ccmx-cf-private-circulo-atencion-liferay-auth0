package com.circulo.auth0.jaxrs;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.model.Auth0TokenResult;
import com.circulo.auth0.model.OidcUserClaims;
import com.circulo.auth0.service.Auth0LoginTokenService;
import com.circulo.auth0.service.Auth0TokenClient;
import com.circulo.auth0.service.IdTokenValidator;
import com.circulo.auth0.service.SessionTokenStore;
import com.circulo.auth0.security.PortalAccessDeniedException;
import com.circulo.auth0.service.UserProvisioningService;
import com.circulo.auth0.service.UserTokenStore;
import com.circulo.auth0.util.Auth0OAuthUrls;
import com.circulo.auth0.util.CookieUtil;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * {@code GET /o/auth/callback} — intercambio de código, validación de id_token, usuario Liferay,
 * cookie de puente para {@code AutoLogin} y redirección según configuración
 * {@link Auth0IntegrationConfiguration#postLoginRedirectPath()}.
 */
@Component(
	configurationPid = Auth0IntegrationConfiguration.PID,
	immediate = true,
	property = {
		JaxRsWhiteboardProperties.APPLICATION_SELECT + "=(osgi.jaxrs.name=Circulo.Auth0)",
		JaxRsWhiteboardProperties.RESOURCE + "=true"
	},
	service = Object.class
)
@Path("/callback")
public class Auth0CallbackResource {

	private static final Log _log = LogFactoryUtil.getLog(Auth0CallbackResource.class);

	private volatile Auth0IntegrationConfiguration _configuration;

	@Reference
	private Auth0TokenClient _auth0TokenClient;

	@Reference
	private IdTokenValidator _idTokenValidator;

	@Reference
	private UserProvisioningService _userProvisioningService;

	@Reference
	private Auth0LoginTokenService _auth0LoginTokenService;

	@Reference
	private SessionTokenStore _sessionTokenStore;

	@Reference
	private UserTokenStore _userTokenStore;

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			Auth0IntegrationConfiguration.class, properties);
	}

	@GET
	@Produces(MediaType.WILDCARD)
	public Response callback(
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse,
			@QueryParam("code") String code,
			@QueryParam("state") String state,
			@QueryParam("error") String error,
			@QueryParam("error_description") String errorDescription) {

		if (Validator.isNotNull(error)) {
			_log.warn(
				"Auth0 callback con error=" + error +
					" (descripción omitida en log)");

			return _badRequest(
				"Error OAuth desde Auth0: " + error + _safeDescriptionSuffix(
					errorDescription));
		}

		if (Validator.isBlank(code) || Validator.isBlank(state)) {
			return _badRequest("Parámetros code y state son obligatorios");
		}

		Auth0IntegrationConfiguration configuration = _configuration;

		if (configuration == null) {
			_log.error("Auth0 callback: configuración OSGi no disponible");

			return _serverError(
				"Configuración Auth0 no disponible. Compruebe System Settings / OSGi.");
		}

		HttpServletRequest originalRequest =
			PortalUtil.getOriginalServletRequest(httpServletRequest);

		String expectedState = CookieUtil.getCookie(
			originalRequest, Auth0Constants.AUTH0_STATE);

		if (Validator.isBlank(expectedState) || !expectedState.equals(state)) {
			_log.error(
				"Auth0 callback: state no coincide con el cookie (posible CSRF)");

			return _forbidden("State no válido");
		}

		String codeVerifier = CookieUtil.getCookie(
			originalRequest, Auth0Constants.AUTH0_CODE_VERIFIER);
		String nonce = CookieUtil.getCookie(
			originalRequest, Auth0Constants.AUTH0_NONCE);

		if (Validator.isBlank(codeVerifier) || Validator.isBlank(nonce)) {
			return _badRequest(
				"Datos PKCE ausentes; reinicie el login desde /o/auth/login");
		}

		boolean secureCookies = configuration.cookiesSecure();
		String sameSite = configuration.cookieSameSite();

		try {
			Auth0TokenResult tokens = _auth0TokenClient.exchangeAuthorizationCode(
				configuration, code, codeVerifier, configuration.redirectUri());

			if (Validator.isBlank(tokens.getIdToken())) {
				throw new IllegalStateException(
					"La respuesta de token no incluye id_token");
			}

			OidcUserClaims oidcClaims = _idTokenValidator.validateAndExtractClaims(
				configuration, tokens.getIdToken(), nonce);

			Map<String, Object> claimMap = _toClaimMap(oidcClaims);

			long userId = _userProvisioningService.provisionOrUpdateUser(
				httpServletRequest, oidcClaims.getSub(), claimMap);

			String loginToken = _auth0LoginTokenService.generateToken(userId);

			CookieUtil.addCookie(
				httpServletResponse, Auth0Constants.AUTH0_LOGIN_TOKEN, loginToken,
				Auth0Constants.AUTH0_LOGIN_TOKEN_MAX_AGE_SECONDS, secureCookies,
				sameSite);

			HttpSession session = originalRequest.getSession(true);

			_sessionTokenStore.saveTokens(
				session, tokens.getAccessToken(), tokens.getIdToken(),
				tokens.getRefreshToken(), tokens.getExpiresInSeconds());

			long expiresInSeconds = tokens.getExpiresInSeconds();

			long expiresAt =
				System.currentTimeMillis() + (expiresInSeconds * 1000L);

			_userTokenStore.saveToken(
				userId, tokens.getAccessToken(), expiresAt);

			_clearOAuthFlowCookies(
				httpServletResponse, secureCookies, sameSite);

			_log.info(
				"Auth0 callback completado; userId=" + userId + " sub=" +
					oidcClaims.getSub());

			String portalUrl = PortalUtil.getPortalURL(
				httpServletRequest, httpServletRequest.isSecure());

			String postLoginPath = configuration.postLoginRedirectPath();

			if (Validator.isBlank(postLoginPath)) {
				postLoginPath = "/group/guest/home";
			}

			if (!postLoginPath.startsWith("/")) {
				postLoginPath = "/" + postLoginPath;
			}

			String redirect = portalUrl + postLoginPath;

			return Response.status(Response.Status.FOUND).location(
				URI.create(redirect)).build();
		}
		catch (PortalAccessDeniedException e) {
			_log.warn("Auth0 callback: acceso al portal denegado — " + e.getMessage());

			HttpSession deniedSession = originalRequest.getSession(false);

			if (deniedSession != null) {
				_sessionTokenStore.clear(deniedSession);

				try {
					deniedSession.invalidate();
				}
				catch (IllegalStateException ise) {
					// sesión ya invalidada
				}
			}

			_clearOAuthFlowCookies(
				httpServletResponse, secureCookies, sameSite);

			String returnTo = configuration.portalAccessDeniedReturnUri();

			if (Validator.isBlank(returnTo)) {
				returnTo = PortalUtil.getPortalURL(
					httpServletRequest, httpServletRequest.isSecure());
			}

			String logoutUrl = Auth0OAuthUrls.buildV2LogoutUrl(
				configuration, returnTo);

			return Response.status(Response.Status.FOUND).location(
				URI.create(logoutUrl)).build();
		}
		catch (IllegalStateException | IllegalArgumentException e) {
			_log.error("Auth0 callback: " + e.getMessage(), e);

			return _serverError(e.getMessage());
		}
		catch (Exception e) {
			_log.error("Auth0 callback: fallo inesperado", e);

			return _serverError(
				"Error al completar el inicio de sesión. Intente de nuevo.");
		}
	}

	private static void _clearOAuthFlowCookies(
			HttpServletResponse response, boolean secureCookies, String sameSite) {

		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_STATE, secureCookies, sameSite);
		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_NONCE, secureCookies, sameSite);
		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_CODE_VERIFIER, secureCookies, sameSite);
	}

	private static Map<String, Object> _toClaimMap(OidcUserClaims claims) {
		Map<String, Object> map = new HashMap<>();

		map.put("sub", claims.getSub());
		map.put("email", claims.getEmail());
		map.put("given_name", claims.getGivenName());
		map.put("family_name", claims.getFamilyName());

		if (!Validator.isBlank(claims.getAuthBridgeUsuario())) {
			map.put("auth_bridge_usuario", claims.getAuthBridgeUsuario());
		}

		if (!Validator.isBlank(claims.getAuthBridgeNombre())) {
			map.put("auth_bridge_nombre", claims.getAuthBridgeNombre());
		}

		if (!Validator.isBlank(claims.getAuthBridgeApellidos())) {
			map.put("auth_bridge_apellidos", claims.getAuthBridgeApellidos());
		}

		if (!Validator.isBlank(claims.getAuthBridgeCorreo())) {
			map.put("auth_bridge_correo", claims.getAuthBridgeCorreo());
		}

		return map;
	}

	private static String _safeDescriptionSuffix(String errorDescription) {
		if (Validator.isBlank(errorDescription)) {
			return "";
		}

		return " — " + errorDescription;
	}

	private static Response _badRequest(String message) {
		return Response.status(Response.Status.BAD_REQUEST)
			.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
			.entity(message)
			.build();
	}

	private static Response _forbidden(String message) {
		return Response.status(Response.Status.FORBIDDEN)
			.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
			.entity(message)
			.build();
	}

	private static Response _serverError(String message) {
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
			.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
			.entity(message)
			.build();
	}

}
