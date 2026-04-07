package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.Auth0LoginTokenService;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

/**
 * Almacén en memoria de tokens de login (un solo nodo JVM; en cluster usar store distribuido).
 */
@Component(immediate = true, service = Auth0LoginTokenService.class)
public class Auth0LoginTokenServiceImpl implements Auth0LoginTokenService {

	private static final int TOKEN_BYTES = 32;

	private static final long TTL_MILLIS = 120_000L;

	private final ConcurrentHashMap<String, TokenEntry> _tokens =
		new ConcurrentHashMap<>();

	private final SecureRandom _secureRandom = new SecureRandom();

	@Override
	public String generateToken(long userId) {
		_purgeExpired();

		byte[] raw = new byte[TOKEN_BYTES];

		_secureRandom.nextBytes(raw);

		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

		long expiresAt = System.currentTimeMillis() + TTL_MILLIS;

		_tokens.put(token, new TokenEntry(userId, expiresAt));

		return token;
	}

	@Override
	public boolean validateToken(String token) {
		if (token == null) {
			return false;
		}

		TokenEntry entry = _tokens.get(token);

		if (entry == null) {
			return false;
		}

		return entry._expiresAtMillis >= System.currentTimeMillis();
	}

	@Override
	public Long consumeToken(String token) {
		if (token == null) {
			return null;
		}

		TokenEntry entry = _tokens.remove(token);

		if (entry == null) {
			return null;
		}

		if (entry._expiresAtMillis < System.currentTimeMillis()) {
			return null;
		}

		return entry._userId;
	}

	private void _purgeExpired() {
		long now = System.currentTimeMillis();

		_tokens.entrySet().removeIf(e -> e.getValue()._expiresAtMillis < now);
	}

	private static final class TokenEntry {

		private final long _userId;
		private final long _expiresAtMillis;

		private TokenEntry(long userId, long expiresAtMillis) {
			_userId = userId;
			_expiresAtMillis = expiresAtMillis;
		}

	}

}
