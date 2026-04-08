package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.UserTokenStore;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;

import java.io.Serializable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Access tokens por usuario en caché {@link MultiVMPool}, visible en todo el clúster (misma
 * semántica que el antiguo mapa en memoria, sin depender del nodo que ejecutó el callback).
 */
@Component(immediate = true, service = UserTokenStore.class)
public class UserTokenStoreImpl implements UserTokenStore {

	private static final String CACHE_NAME =
		"com.circulo.auth0.cluster.UserApiAccessToken";

	private static final int MIN_TTL_SECONDS = 1;

	@Reference
	private MultiVMPool _multiVMPool;

	private PortalCache<Long, Serializable> _portalCache;

	@Activate
	protected void activate() {
		_portalCache = _toLongKeyCache(
			_multiVMPool.getPortalCache(CACHE_NAME));
	}

	@SuppressWarnings("unchecked")
	private static PortalCache<Long, Serializable> _toLongKeyCache(
			PortalCache<? extends Serializable, ? extends Serializable> cache) {

		return (PortalCache<Long, Serializable>)cache;
	}

	@Override
	public void saveToken(long userId, String accessToken, long expiresAt) {
		if (userId <= 0 || accessToken == null) {
			return;
		}

		long ttlMillis = expiresAt - System.currentTimeMillis();

		long ttlSecondsLong = ttlMillis / 1000L;

		if (ttlSecondsLong < MIN_TTL_SECONDS) {
			ttlSecondsLong = MIN_TTL_SECONDS;
		}

		if (ttlSecondsLong > Integer.MAX_VALUE) {
			ttlSecondsLong = Integer.MAX_VALUE;
		}

		int ttlSeconds = (int)ttlSecondsLong;

		UserAccessTokenEntry entry = new UserAccessTokenEntry(
			accessToken, expiresAt);

		_portalCache.put(userId, entry, ttlSeconds);
	}

	@Override
	public String getToken(long userId) {
		if (userId <= 0) {
			return null;
		}

		Serializable raw = _portalCache.get(userId);

		if (!(raw instanceof UserAccessTokenEntry)) {
			return null;
		}

		UserAccessTokenEntry entry = (UserAccessTokenEntry)raw;

		if (entry._expiresAtMillis <= System.currentTimeMillis()) {
			_portalCache.remove(userId);

			return null;
		}

		return entry._token;
	}

	@Override
	public void removeToken(long userId) {
		if (userId > 0) {
			_portalCache.remove(userId);
		}
	}

	private static final class UserAccessTokenEntry implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String _token;
		private final long _expiresAtMillis;

		private UserAccessTokenEntry(String token, long expiresAtMillis) {
			_token = token;
			_expiresAtMillis = expiresAtMillis;
		}

	}

}
