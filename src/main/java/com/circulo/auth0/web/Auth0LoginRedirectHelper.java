package com.circulo.auth0.web;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.util.Auth0AuthorizeUrlBuilder;
import com.circulo.auth0.util.CookieUtil;
import com.circulo.auth0.util.OAuthRandomTokenUtil;
import com.circulo.auth0.util.PKCEUtil;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Orquesta el inicio del flujo OAuth: PKCE, cookies httpOnly temporales y URL de {@code /authorize}.
 * <p>
 * No registra valores de cookies, secretos ni host de autorización en INFO (solo DEBUG genérico).
 */
public final class Auth0LoginRedirectHelper {

	private static final Log _log = LogFactoryUtil.getLog(
		Auth0LoginRedirectHelper.class);

	private Auth0LoginRedirectHelper() {
	}

	/**
	 * Valida configuración, genera PKCE/state/nonce, los persiste en cookies httpOnly y devuelve
	 * la URI de redirección a Auth0.
	 */
	public static URI beginAuthorization(
			HttpServletRequest request, HttpServletResponse response,
			Auth0IntegrationConfiguration configuration) {

		Auth0AuthorizeUrlBuilder.validateConfigurationForLogin(configuration);

		String state = OAuthRandomTokenUtil.generateOpaqueToken();
		String nonce = OAuthRandomTokenUtil.generateOpaqueToken();
		String codeVerifier = PKCEUtil.generateCodeVerifier();
		String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

		String authorizeUrl = Auth0AuthorizeUrlBuilder.buildAuthorizeUrl(
			configuration, state, nonce, codeChallenge);

		int maxAge = Auth0Constants.OAUTH_FLOW_COOKIE_MAX_AGE_SECONDS;

		boolean secureCookies = configuration.cookiesSecure();
		String sameSite = configuration.cookieSameSite();

		CookieUtil.addCookie(
			response, Auth0Constants.AUTH0_STATE, state, maxAge, secureCookies,
			sameSite);
		CookieUtil.addCookie(
			response, Auth0Constants.AUTH0_NONCE, nonce, maxAge, secureCookies,
			sameSite);
		CookieUtil.addCookie(
			response, Auth0Constants.AUTH0_CODE_VERIFIER, codeVerifier, maxAge,
			secureCookies, sameSite);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Inicio flujo Auth0 (PKCE); redirección a /authorize " +
					"(host y query omitidos en log)");
		}

		return URI.create(authorizeUrl);
	}

}
