package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.Auth0LoginTokenService;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;

import java.io.Serializable;

import java.security.SecureRandom;
import java.util.Base64;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Tokens de un solo uso para el puente callback → {@code AutoLogin}, replicados vía {@link
 * MultiVMPool} para que cualquier nodo del clúster pueda validarlos y consumirlos.
 */
@Component(immediate = true, service = Auth0LoginTokenService.class)
public class Auth0LoginTokenServiceImpl implements Auth0LoginTokenService {

	private static final int TOKEN_BYTES = 32;

	private static final int TOKEN_TTL_SECONDS = 120;

	private static final String CACHE_NAME =
		"com.circulo.auth0.cluster.LoginBridgeToken";

	private final SecureRandom _secureRandom = new SecureRandom();

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
	public String generateToken(long userId) {
		byte[] raw = new byte[TOKEN_BYTES];

		_secureRandom.nextBytes(raw);

		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

		long expiresAtMillis = System.currentTimeMillis() + (TOKEN_TTL_SECONDS * 1000L);

		LoginBridgeEntry entry = new LoginBridgeEntry(userId, expiresAtMillis);

		_portalCache.put(token, entry, TOKEN_TTL_SECONDS);

		return token;
	}

	@Override
	public boolean validateToken(String token) {
		if (token == null) {
			return false;
		}

		Serializable raw = _portalCache.get(token);

		if (!(raw instanceof LoginBridgeEntry)) {
			return false;
		}

		LoginBridgeEntry entry = (LoginBridgeEntry)raw;

		return entry._expiresAtMillis >= System.currentTimeMillis();
	}

	@Override
	public Long consumeToken(String token) {
		if (token == null) {
			return null;
		}

		Serializable raw = _portalCache.get(token);

		if (!(raw instanceof LoginBridgeEntry)) {
			return null;
		}

		_portalCache.remove(token);

		LoginBridgeEntry entry = (LoginBridgeEntry)raw;

		if (entry._expiresAtMillis < System.currentTimeMillis()) {
			return null;
		}

		return entry._userId;
	}

	private static final class LoginBridgeEntry implements Serializable {

		private static final long serialVersionUID = 1L;

		private final long _userId;
		private final long _expiresAtMillis;

		private LoginBridgeEntry(long userId, long expiresAtMillis) {
			_userId = userId;
			_expiresAtMillis = expiresAtMillis;
		}

	}

}
