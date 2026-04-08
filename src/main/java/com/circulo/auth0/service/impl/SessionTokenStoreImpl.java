package com.circulo.auth0.service.impl;

import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.SessionTokenStore;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;

import java.io.Serializable;

import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Tokens OAuth asociados al {@link HttpSession#getId() id de sesión} en {@link MultiVMPool}, de
 * modo que cualquier nodo del clúster Liferay puede leerlos si comparten el mismo id de sesión
 * (replicación de sesión o afinidad). Ya no se guardan en atributos de {@link HttpSession}.
 */
@Component(immediate = true, service = SessionTokenStore.class)
public class SessionTokenStoreImpl implements SessionTokenStore {

	private static final String CACHE_NAME =
		"com.circulo.auth0.cluster.SessionOAuthTokens";

	private static final int MIN_TTL_SECONDS = 1;

	@Reference
	private MultiVMPool _multiVMPool;

	private PortalCache<String, Serializable> _portalCache;

	@Activate
	protected void activate() {
		_portalCache = _toStringKeyCache(
			_multiVMPool.getPortalCache(CACHE_NAME));
	}

	@SuppressWarnings("unchecked")
	private static PortalCache<String, Serializable> _toStringKeyCache(
			PortalCache<? extends Serializable, ? extends Serializable> cache) {

		return (PortalCache<String, Serializable>)cache;
	}

	@Override
	public void saveTokens(
			HttpSession httpSession, String accessToken, String idToken,
			String refreshToken, long expiresInSeconds) {

		if (httpSession == null) {
			return;
		}

		String sessionId = httpSession.getId();

		_removeLegacySessionAttributes(httpSession);

		long expiresAtEpochSeconds = 0L;

		if (expiresInSeconds > 0) {
			expiresAtEpochSeconds =
				(System.currentTimeMillis() / 1000L) + expiresInSeconds;
		}

		OAuthSessionBundle bundle = new OAuthSessionBundle(
			accessToken, idToken, refreshToken, expiresAtEpochSeconds);

		int ttlSeconds = _ttlSeconds(expiresInSeconds);

		_portalCache.put(sessionId, bundle, ttlSeconds);
	}

	@Override
	public void clear(HttpSession httpSession) {

		if (httpSession == null) {
			return;
		}

		_portalCache.remove(httpSession.getId());

		_removeLegacySessionAttributes(httpSession);
	}

	@Override
	public String getAccessToken(HttpSession httpSession) {

		if (httpSession == null) {
			return null;
		}

		Serializable raw = _portalCache.get(httpSession.getId());

		if (!(raw instanceof OAuthSessionBundle)) {
			return _legacyAccessToken(httpSession);
		}

		OAuthSessionBundle bundle = (OAuthSessionBundle)raw;

		if ((bundle._accessTokenExpiresAtEpochSeconds > 0) &&
			((System.currentTimeMillis() / 1000L) >=
				bundle._accessTokenExpiresAtEpochSeconds)) {

			_portalCache.remove(httpSession.getId());

			return null;
		}

		return bundle._accessToken;
	}

	private static String _legacyAccessToken(HttpSession httpSession) {
		Object v = httpSession.getAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN);

		return (v instanceof String) ? (String)v : null;
	}

	private static void _removeLegacySessionAttributes(HttpSession httpSession) {

		httpSession.removeAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_ID_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_REFRESH_TOKEN);
		httpSession.removeAttribute(Auth0Constants.AUTH0_ACCESS_TOKEN_EXPIRES_AT);
		httpSession.removeAttribute(Auth0Constants.AUTH0_CODE_VERIFIER);
		httpSession.removeAttribute(Auth0Constants.AUTH0_STATE);
		httpSession.removeAttribute(Auth0Constants.AUTH0_NONCE);
	}

	private static int _ttlSeconds(long expiresInSeconds) {
		long ttl = (expiresInSeconds > 0) ? expiresInSeconds : 3600L;

		if (ttl < MIN_TTL_SECONDS) {
			ttl = MIN_TTL_SECONDS;
		}

		if (ttl > Integer.MAX_VALUE) {
			ttl = Integer.MAX_VALUE;
		}

		return (int)ttl;
	}

	private static final class OAuthSessionBundle implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String _accessToken;
		private final String _idToken;
		private final String _refreshToken;
		private final long _accessTokenExpiresAtEpochSeconds;

		private OAuthSessionBundle(
				String accessToken, String idToken, String refreshToken,
				long accessTokenExpiresAtEpochSeconds) {

			_accessToken = accessToken;
			_idToken = idToken;
			_refreshToken = refreshToken;
			_accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds;
		}

	}

}
