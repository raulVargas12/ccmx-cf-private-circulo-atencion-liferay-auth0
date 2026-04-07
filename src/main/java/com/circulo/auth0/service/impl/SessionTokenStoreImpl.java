package com.circulo.auth0.service.impl;

import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.SessionTokenStore;

import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;

/**
 * Tokens OAuth en {@link HttpSession} (solo servidor).
 */
@Component(immediate = true, service = SessionTokenStore.class)
public class SessionTokenStoreImpl implements SessionTokenStore {

	@Override
	public void saveTokens(
		HttpSession httpSession, String accessToken, String idToken,
		String refreshToken, long expiresInSeconds) {

		httpSession.setAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN, accessToken);

		if (idToken != null) {
			httpSession.setAttribute(Auth0Constants.AUTH0_ID_TOKEN, idToken);
		}

		if (refreshToken != null) {
			httpSession.setAttribute(Auth0Constants.AUTH0_REFRESH_TOKEN, refreshToken);
		}

		long expiresAt = 0L;

		if (expiresInSeconds > 0) {
			expiresAt = (System.currentTimeMillis() / 1000L) + expiresInSeconds;
		}

		httpSession.setAttribute(
			Auth0Constants.AUTH0_ACCESS_TOKEN_EXPIRES_AT, Long.valueOf(expiresAt));
	}

	@Override
	public void clear(HttpSession httpSession) {

		httpSession.removeAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_ID_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_REFRESH_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN_EXPIRES_AT);
		httpSession.removeAttribute(Auth0Constants.AUTH0_CODE_VERIFIER);
		httpSession.removeAttribute(Auth0Constants.AUTH0_STATE);
		httpSession.removeAttribute(Auth0Constants.AUTH0_NONCE);
	}

	@Override
	public String getAccessToken(HttpSession httpSession) {

		Object v = httpSession.getAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN);

		return (v instanceof String) ? (String)v : null;
	}

}
