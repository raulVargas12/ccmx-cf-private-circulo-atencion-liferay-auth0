package com.circulo.auth0.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Genera valores opacos URL-safe (state, nonce) con alta entropía.
 */
public final class OAuthRandomTokenUtil {

	private static final int DEFAULT_ENTROPY_BYTES = 32;

	private static final SecureRandom _secureRandom = new SecureRandom();

	private OAuthRandomTokenUtil() {
	}

	/**
	 * Token aleatorio (~32 bytes de entropía) codificado en Base64URL sin padding.
	 * Apto para {@code state} y {@code nonce} en OIDC.
	 */
	public static String generateOpaqueToken() {
		byte[] bytes = new byte[DEFAULT_ENTROPY_BYTES];

		_secureRandom.nextBytes(bytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
