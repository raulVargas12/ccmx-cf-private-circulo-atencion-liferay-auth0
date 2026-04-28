package com.circulo.auth0.jaxrs;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.SessionTokenStore;
import com.circulo.auth0.service.UserTokenStore;
import com.circulo.auth0.util.Auth0OAuthUrls;
import com.circulo.auth0.util.CookieUtil;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * {@code POST /o/auth/logout} — invalida sesión Liferay, limpia cookies/tokens y redirige al
 * logout federado de Auth0.
 */
@Component(
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	configurationPid = Auth0IntegrationConfiguration.PID,
	immediate = true,
	property = {
		JaxRsWhiteboardProperties.APPLICATION_SELECT + "=(osgi.jaxrs.name=Circulo.Auth0)",
		JaxRsWhiteboardProperties.RESOURCE + "=true"
	},
	service = Object.class
)
@Path("/logout")
public class Auth0LogoutResource {

	private static final Log _log = LogFactoryUtil.getLog(Auth0LogoutResource.class);

	private volatile Auth0IntegrationConfiguration _configuration;

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

	@POST
	@Produces(MediaType.WILDCARD)
	public Response logout(
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse) {

		Auth0IntegrationConfiguration configuration = _configuration;

		if (configuration == null) {
			_log.error("Auth0 logout: configuración OSGi no disponible");

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity(
					"Configuración Auth0 no disponible. Compruebe System Settings / OSGi.")
				.build();
		}

		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(
			httpServletRequest);

		if (!_isSameOriginPost(originalRequest)) {
			_log.warn("Auth0 logout rechazado por validación CSRF (origin/referer)");

			return Response.status(Response.Status.FORBIDDEN)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity("No se pudo validar la solicitud de cierre de sesión.")
				.build();
		}

		long userId = PortalUtil.getUserId(originalRequest);

		if (userId > 0) {
			_userTokenStore.removeToken(userId);
		}

		HttpSession session = originalRequest.getSession(false);

		if (session != null) {
			_sessionTokenStore.clear(session);

			session.invalidate();
		}

		boolean secureCookies = configuration.cookiesSecure();
		String sameSite = configuration.cookieSameSite();

		_clearOAuthCookies(
			httpServletResponse, secureCookies, sameSite);

		String returnTo = configuration.logoutReturnUri();

		if (Validator.isBlank(returnTo)) {
			returnTo = PortalUtil.getPortalURL(
				httpServletRequest, httpServletRequest.isSecure());
		}

		try {
			String logoutUrl = Auth0OAuthUrls.buildV2LogoutUrl(
				configuration, returnTo);

			_log.info("Auth0 logout: redirección a logout federado (URL omitida en log)");

			return Response.status(Response.Status.FOUND).location(
				URI.create(logoutUrl)).build();
		}
		catch (IllegalStateException e) {
			_log.error("Auth0 logout: error al construir URL de logout federado", e);

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity("No fue posible completar el cierre de sesión.")
				.build();
		}
	}

	private static boolean _isSameOriginPost(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		String referer = request.getHeader("Referer");

		if (Validator.isBlank(origin) && Validator.isBlank(referer)) {
			return false;
		}

		String portalUrl = PortalUtil.getPortalURL(request, request.isSecure());

		try {
			URI expected = URI.create(portalUrl);
			URI candidate = Validator.isNotNull(origin) ? URI.create(origin) : URI.create(referer);

			return _sameOrigin(expected, candidate);
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean _sameOrigin(URI expected, URI candidate) {
		if ((expected == null) || (candidate == null)) {
			return false;
		}

		if (!String.valueOf(expected.getScheme()).equalsIgnoreCase(
				String.valueOf(candidate.getScheme()))) {

			return false;
		}

		if (!String.valueOf(expected.getHost()).equalsIgnoreCase(
				String.valueOf(candidate.getHost()))) {

			return false;
		}

		return _normalizePort(expected) == _normalizePort(candidate);
	}

	private static int _normalizePort(URI uri) {
		int port = uri.getPort();

		if (port >= 0) {
			return port;
		}

		if ("https".equalsIgnoreCase(uri.getScheme())) {
			return 443;
		}

		if ("http".equalsIgnoreCase(uri.getScheme())) {
			return 80;
		}

		return -1;
	}

	private static void _clearOAuthCookies(
			HttpServletResponse response, boolean secureCookies, String sameSite) {

		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_STATE, secureCookies, sameSite);
		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_NONCE, secureCookies, sameSite);
		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_CODE_VERIFIER, secureCookies, sameSite);
		CookieUtil.clearCookie(
			response, Auth0Constants.AUTH0_LOGIN_TOKEN, secureCookies, sameSite);
	}

}
