package com.circulo.auth0.security;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.Auth0LoginTokenService;
import com.circulo.auth0.util.CookieUtil;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * Completa el login en Liferay tras Auth0: consume {@value Auth0Constants#AUTH0_LOGIN_TOKEN} y
 * devuelve credenciales para el pipeline oficial del portal.
 */
@Component(
	configurationPid = Auth0IntegrationConfiguration.PID, immediate = true,
	service = AutoLogin.class
)
public class Auth0AutoLogin implements AutoLogin {

	private volatile Auth0IntegrationConfiguration _configuration;

	@Reference
	private Auth0LoginTokenService _auth0LoginTokenService;

	@Reference
	private UserLocalService _userLocalService;

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			Auth0IntegrationConfiguration.class, properties);
	}

	@Override
	public String[] login(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws AutoLoginException {

		Auth0IntegrationConfiguration configuration = _configuration;

		boolean secureCookies =
			(configuration != null) && configuration.cookiesSecure();

		String sameSite =
			(configuration != null) ? configuration.cookieSameSite() : "";

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

			CookieUtil.clearCookie(
				httpServletResponse, Auth0Constants.AUTH0_LOGIN_TOKEN,
				secureCookies, sameSite);

			return new String[] {
				String.valueOf(userId), user.getPassword(), String.valueOf(true)
			};
		}
		catch (Exception e) {
			throw new AutoLoginException(e);
		}
	}

}
