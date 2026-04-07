package com.circulo.auth0.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE RFC 7636: {@code code_verifier} y {@code code_challenge} (S256).
 */
public final class PKCEUtil {

	/**
	 * Longitud mínima del verifier en caracteres (tras codificar bytes aleatorios en Base64URL).
	 */
	private static final int VERIFIER_MIN_BYTES = 32;

	/**
	 * Longitud máxima en bytes que, codificada en Base64URL sin padding, no supera 128 caracteres
	 * (96 * 4 / 3 = 128).
	 */
	private static final int VERIFIER_MAX_BYTES = 96;

	private static final SecureRandom _secureRandom = new SecureRandom();

	private PKCEUtil() {
	}

	/**
	 * Genera un {@code code_verifier} con longitud entre 43 y 128 caracteres (Base64URL sin padding).
	 */
	public static String generateCodeVerifier() {
		int numBytes =
			VERIFIER_MIN_BYTES + _secureRandom.nextInt(
				VERIFIER_MAX_BYTES - VERIFIER_MIN_BYTES + 1);

		byte[] bytes = new byte[numBytes];

		_secureRandom.nextBytes(bytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * {@code code_challenge} = BASE64URL(SHA256(verifier)) sin padding (método S256).
	 */
	public static String generateCodeChallenge(String codeVerifier) {
		if (codeVerifier == null || codeVerifier.isEmpty()) {
			throw new IllegalArgumentException("codeVerifier must not be empty");
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(
				codeVerifier.getBytes(StandardCharsets.US_ASCII));

			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	/**
	 * @deprecated Usar {@link #generateCodeChallenge(String)}.
	 */
	@Deprecated
	public static String deriveCodeChallengeS256(String codeVerifier) {
		return generateCodeChallenge(codeVerifier);
	}

}
