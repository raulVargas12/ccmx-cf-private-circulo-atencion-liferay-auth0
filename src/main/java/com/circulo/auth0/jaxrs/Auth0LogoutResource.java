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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * {@code GET /o/auth/logout} — invalida sesión Liferay, limpia cookies/tokens y redirige al
 * logout federado de Auth0.
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

	@GET
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
			_log.error("Auth0 logout: " + e.getMessage());

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity(e.getMessage())
				.build();
		}
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
