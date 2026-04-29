package com.circulo.auth0.security.crypto;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado simétrico AES/GCM para proteger tokens en clúster.
 * La clave se deriva de la configuración OSGi (tokenEncryptionKey) para que todos los nodos
 * puedan cifrar y descifrar los mismos tokens.
 */
public class TokenCipher {

	private static final Log _log = LogFactoryUtil.getLog(TokenCipher.class);

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;

	private static final ConcurrentHashMap<String, SecretKey> _keyCache = new ConcurrentHashMap<>();

	private static SecretKey _deriveKey(String encryptionKey) throws Exception {
		if (Validator.isBlank(encryptionKey)) {
			throw new IllegalArgumentException("La clave de cifrado no puede estar vacía");
		}

		return _keyCache.computeIfAbsent(encryptionKey, key -> {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
				return new SecretKeySpec(hash, "AES");
			}
			catch (Exception e) {
				throw new RuntimeException("Error derivando clave AES", e);
			}
		});
	}

	public static String encrypt(String plainText, String encryptionKey) {
		if (Validator.isBlank(plainText)) {
			return plainText;
		}

		try {
			SecretKey secretKey = _deriveKey(encryptionKey);
			
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

			byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			byte[] message = new byte[GCM_IV_LENGTH + cipherText.length];
			System.arraycopy(iv, 0, message, 0, GCM_IV_LENGTH);
			System.arraycopy(cipherText, 0, message, GCM_IV_LENGTH, cipherText.length);

			return Base64.getEncoder().encodeToString(message);
		}
		catch (Exception e) {
			_log.error("Error al cifrar token", e);
			return null;
		}
	}

	public static String decrypt(String encryptedText, String encryptionKey) {
		if (Validator.isBlank(encryptedText)) {
			return encryptedText;
		}

		try {
			SecretKey secretKey = _deriveKey(encryptionKey);
			
			byte[] message = Base64.getDecoder().decode(encryptedText);

			if (message.length < GCM_IV_LENGTH) {
				return null;
			}

			byte[] iv = new byte[GCM_IV_LENGTH];
			System.arraycopy(message, 0, iv, 0, GCM_IV_LENGTH);

			byte[] cipherText = new byte[message.length - GCM_IV_LENGTH];
			System.arraycopy(message, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

			byte[] plainText = cipher.doFinal(cipherText);

			return new String(plainText, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error al descifrar token (puede deberse a rotación de clave o error de formato): " + e.getMessage());
			}
			return null;
		}
	}
}
