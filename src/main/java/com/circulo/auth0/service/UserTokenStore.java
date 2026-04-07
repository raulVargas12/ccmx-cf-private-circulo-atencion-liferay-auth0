package com.circulo.auth0.service;

/**
 * Almacén de {@code access_token} por {@code userId} (fallback cuando no hay sesión HTTP
 * compartida, p. ej. proxy u otros módulos).
 */
public interface UserTokenStore {

	/**
	 * @param expiresAt epoch millis en que deja de considerarse válido el token
	 */
	void saveToken(long userId, String accessToken, long expiresAt);

	/**
	 * Devuelve el access token si existe y no ha caducado; si expiró, lo elimina y devuelve {@code null}.
	 */
	String getToken(long userId);

	void removeToken(long userId);

}
