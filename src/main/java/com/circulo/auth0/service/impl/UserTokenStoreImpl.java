package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.UserTokenStore;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

/**
 * Almacén en memoria por JVM; en cluster usar implementación distribuida.
 */
@Component(immediate = true, service = UserTokenStore.class)
public class UserTokenStoreImpl implements UserTokenStore {

	private final ConcurrentHashMap<Long, TokenEntry> _entries =
		new ConcurrentHashMap<>();

	@Override
	public void saveToken(long userId, String accessToken, long expiresAt) {
		if (userId <= 0 || accessToken == null) {
			return;
		}

		_entries.put(userId, new TokenEntry(accessToken, expiresAt));
	}

	@Override
	public String getToken(long userId) {
		if (userId <= 0) {
			return null;
		}

		TokenEntry entry = _entries.get(userId);

		if (entry == null) {
			return null;
		}

		if (entry._expiresAtMillis <= System.currentTimeMillis()) {
			_entries.remove(userId, entry);

			return null;
		}

		return entry._token;
	}

	@Override
	public void removeToken(long userId) {
		if (userId > 0) {
			_entries.remove(userId);
		}
	}

	private static final class TokenEntry {

		private final String _token;
		private final long _expiresAtMillis;

		private TokenEntry(String token, long expiresAtMillis) {
			_token = token;
			_expiresAtMillis = expiresAtMillis;
		}

	}

}
