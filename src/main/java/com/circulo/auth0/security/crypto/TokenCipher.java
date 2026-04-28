package com.circulo.auth0.security.crypto;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifrado simétrico AES/GCM para proteger tokens en memoria.
 * La clave se genera dinámicamente en memoria y no se persiste.
 */
public class TokenCipher {

	private static final Log _log = LogFactoryUtil.getLog(TokenCipher.class);

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;

	private static SecretKey _secretKey;

	static {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256, new SecureRandom());
			_secretKey = keyGen.generateKey();

			if (_log.isDebugEnabled()) {
				_log.debug("TokenCipher inicializado con clave AES 256 en memoria");
			}
		}
		catch (Exception e) {
			_log.error("Error inicializando TokenCipher", e);
		}
	}

	public static String encrypt(String plainText) {
		if (Validator.isBlank(plainText)) {
			return plainText;
		}

		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, _secretKey, parameterSpec);

			byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

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

	public static String decrypt(String encryptedText) {
		if (Validator.isBlank(encryptedText)) {
			return encryptedText;
		}

		try {
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
			cipher.init(Cipher.DECRYPT_MODE, _secretKey, parameterSpec);

			byte[] plainText = cipher.doFinal(cipherText);

			return new String(plainText, "UTF-8");
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error al descifrar token (puede deberse a rotación de clave o error de formato): " + e.getMessage());
			}
			return null;
		}
	}
}
