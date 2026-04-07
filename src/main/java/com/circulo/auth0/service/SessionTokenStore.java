package com.circulo.auth0.service;

import javax.servlet.http.HttpSession;

/**
 * Persistencia de tokens OAuth en la sesión HTTP (o abstracción futura).
 * <p>
 * TODO: definir política de almacenamiento (solo servidor, cifrado, refresh, revocación).
 */
public interface SessionTokenStore {

	/**
	 * Guarda tokens asociados a la sesión del usuario.
	 */
	void saveTokens(
		HttpSession httpSession, String accessToken, String idToken,
		String refreshToken, long expiresInSeconds);

	/**
	 * Limpia credenciales almacenadas (logout).
	 */
	void clear(HttpSession httpSession);

	/**
	 * Obtiene access token vigente si existe; {@code null} si no hay sesión OAuth.
	 */
	String getAccessToken(HttpSession httpSession);

}
