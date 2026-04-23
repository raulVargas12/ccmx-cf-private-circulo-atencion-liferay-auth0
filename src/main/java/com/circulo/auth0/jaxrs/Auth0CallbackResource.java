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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
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
 * {@code GET /o/auth/callback} — si Auth0 devuelve {@code error} / {@code error_description},
 * redirige vía {@code /v2/logout} de Auth0 (cierra sesión IdP) y {@code returnTo} a la página
 * configurada en {@link Auth0IntegrationConfiguration#auth0OAuthErrorPagePath()} con {@code ?code=}
 * para el portlet React {@code auth0_error}; si {@code email_not_verified}, a
 * {@link Auth0IntegrationConfiguration#auth0EmailNotVerifiedPagePath()}; si no, intercambio de código,
 * validación de {@code id_token}, usuario Liferay, cookie de puente para {@code AutoLogin} y
 * redirección según {@link Auth0IntegrationConfiguration#postLoginRedirectPath()}.
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
			@QueryParam("state") String state) {

		HttpServletRequest originalRequest =
			PortalUtil.getOriginalServletRequest(httpServletRequest);

		String error = originalRequest.getParameter("error");
		String errorDescription = originalRequest.getParameter("error_description");

		if (Validator.isNotNull(error)) {
			String desc =
				(errorDescription != null) ?
					errorDescription.toLowerCase(Locale.ROOT) : "";

			if ("access_denied".equals(error) &&
				desc.contains("email_not_verified")) {

				_log.warn("Auth0 login falló: email no verificado");

				return _seeOtherViaAuth0LogoutThenFriendlyPage(
					httpServletRequest, _configuration,
					_emailNotVerifiedPagePath());
			}

			if ("access_denied".equals(error)) {
				_log.warn("Auth0 login denegado por el usuario");

				return _seeOtherViaAuth0LogoutThenFriendlyPage(
					httpServletRequest, _configuration,
					_auth0OAuthErrorRedirectWithCode("access_denied"));
			}

			_log.error("Error en callback Auth0: " + error);

			return _seeOtherViaAuth0LogoutThenFriendlyPage(
				httpServletRequest, _configuration,
				_auth0OAuthErrorRedirectWithCode(error));
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

			if (_log.isDebugEnabled()) {
				_log.debug("Auth0 callback completado; userId=" + userId);
			}

			_log.info("Auth0 callback completado");

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

			return _seeOtherViaAuth0LogoutThenFriendlyPage(
				httpServletRequest, configuration,
				_auth0OAuthErrorRedirectWithCode("portal_access_denied"));
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

	/**
	 * Ruta relativa al portal (p. ej. {@code /web/guest/error-auth}) donde está el portlet
	 * {@code auth0_error}; se añade {@code ?code=} o {@code &code=} según corresponda.
	 */
	private String _auth0OAuthErrorPagePath() {
		Auth0IntegrationConfiguration configuration = _configuration;

		if (configuration == null) {
			return "/web/guest/error-auth";
		}

		String path = configuration.auth0OAuthErrorPagePath();

		if (Validator.isBlank(path)) {
			return "/web/guest/error-auth";
		}

		path = path.trim();

		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		return path;
	}

	/**
	 * Ruta relativa al portal cuando Auth0 devuelve {@code access_denied} por email no verificado.
	 */
	private String _emailNotVerifiedPagePath() {
		Auth0IntegrationConfiguration configuration = _configuration;

		if (configuration == null) {
			return "/web/guest/email-no-verificado";
		}

		String path = configuration.auth0EmailNotVerifiedPagePath();

		if (Validator.isBlank(path)) {
			return "/web/guest/email-no-verificado";
		}

		path = path.trim();

		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		return path;
	}

	private String _auth0OAuthErrorRedirectWithCode(String rawOAuthErrorCode) {
		String base = _auth0OAuthErrorPagePath();
		String sep = base.contains("?") ? "&" : "?";

		try {
			return base + sep + "code=" + URLEncoder.encode(
				rawOAuthErrorCode, StandardCharsets.UTF_8.name());
		}
		catch (java.io.UnsupportedEncodingException uee) {
			return base + sep + "code=" + rawOAuthErrorCode;
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

	/**
	 * Cierra la sesión en Auth0 ({@code /v2/logout}) y deja {@code returnTo} en la página del portal
	 * indicada, para que un nuevo {@code /authorize} vuelva a pedir credenciales. Si no hay
	 * configuración o {@code clientId}, redirige solo al portal.
	 */
	private static Response _seeOtherViaAuth0LogoutThenFriendlyPage(
			HttpServletRequest httpServletRequest,
			Auth0IntegrationConfiguration configuration,
			String pathWithOptionalQuery) {

		if ((configuration == null) ||
			Validator.isBlank(configuration.clientId())) {

			return _seeOtherToFriendlyPage(
				httpServletRequest, pathWithOptionalQuery);
		}

		String returnToAbsolute = _absolutePortalUrl(
			httpServletRequest, pathWithOptionalQuery);

		try {
			String logoutUrl = Auth0OAuthUrls.buildV2LogoutUrl(
				configuration, returnToAbsolute);

			return Response.seeOther(URI.create(logoutUrl)).build();
		}
		catch (IllegalStateException e) {
			return _seeOtherToFriendlyPage(
				httpServletRequest, pathWithOptionalQuery);
		}
	}

	private static String _absolutePortalUrl(
		HttpServletRequest httpServletRequest, String pathWithOptionalQuery) {

		String portalUrl = PortalUtil.getPortalURL(
			httpServletRequest, httpServletRequest.isSecure());

		String path = pathWithOptionalQuery;

		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		return portalUrl + path;
	}

	private static Response _seeOtherToFriendlyPage(
		HttpServletRequest httpServletRequest, String pathWithOptionalQuery) {

		return Response.seeOther(
			URI.create(
				_absolutePortalUrl(httpServletRequest, pathWithOptionalQuery))
		).build();
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
