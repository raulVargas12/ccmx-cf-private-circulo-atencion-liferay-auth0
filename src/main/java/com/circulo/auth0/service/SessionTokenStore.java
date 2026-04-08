package com.circulo.auth0.service;

import javax.servlet.http.HttpSession;

/**
 * Persistencia de tokens OAuth por id de sesión en caché {@code MultiVMPool} (clúster Liferay).
 * Requiere replicación de sesión o sticky session para que el {@code JSESSIONID} sea coherente
 * entre nodos; ver README del módulo.
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
