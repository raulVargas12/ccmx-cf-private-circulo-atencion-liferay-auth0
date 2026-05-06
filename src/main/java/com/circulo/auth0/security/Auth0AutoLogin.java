package com.circulo.auth0.security;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.Auth0LoginTokenService;
import com.circulo.auth0.util.CookieUtil;

import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;	

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Completa el login en Liferay tras Auth0: consume {@value Auth0Constants#AUTH0_LOGIN_TOKEN} y
 * devuelve credenciales para el pipeline oficial del portal.
 */
@Component(
	immediate = true,
	service = AutoLogin.class
)
public class Auth0AutoLogin implements AutoLogin {

	private static final Log _log = LogFactoryUtil.getLog(Auth0AutoLogin.class);

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Auth0LoginTokenService _auth0LoginTokenService;

	@Reference
	private UserLocalService _userLocalService;

	@Override
	public String[] login(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws AutoLoginException {

		// Si Liferay ya identificó al usuario en esta petición (ej. sesión existente o hilo principal ya lo hizo), no hacemos nada.
		if (Validator.isNotNull(httpServletRequest.getRemoteUser())) {
			return null;
		}

		long companyId = PortalUtil.getCompanyId(httpServletRequest);
		Auth0IntegrationConfiguration configuration;
		try {
			configuration = _configurationProvider.getCompanyConfiguration(
				Auth0IntegrationConfiguration.class, companyId);
		}
		catch (ConfigurationException e) {
			_log.error("Auth0AutoLogin: configuración OSGi no disponible para companyId " + companyId, e);
			return null;
		}

		boolean secureCookies = configuration.cookiesSecure();
		String sameSite = configuration.cookieSameSite();

		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(
			httpServletRequest);

		String token = CookieUtil.getCookie(
			originalRequest, Auth0Constants.AUTH0_LOGIN_TOKEN);

		if (Validator.isBlank(token)) {
			return null;
		}

		Long userId = _auth0LoginTokenService.consumeToken(token);

		if (userId == null) {
			CookieUtil.clearCookie(
				httpServletResponse, Auth0Constants.AUTH0_LOGIN_TOKEN,
				secureCookies, sameSite);

			return null;
		}

		try {
			User user = _userLocalService.getUser(userId);

			_log.info("Token consumido con exito. Autenticando al usuario Liferay ID: " + userId);

			CookieUtil.clearCookie(
				httpServletResponse, Auth0Constants.AUTH0_LOGIN_TOKEN,
				secureCookies, sameSite);

			return new String[] {
				String.valueOf(userId), user.getPassword(), String.valueOf(true)
			};
		}
		catch (Exception e) {
			_log.error("Error al obtener el usuario para el AutoLogin", e);
			throw new AutoLoginException(e);
		}
	}

}
