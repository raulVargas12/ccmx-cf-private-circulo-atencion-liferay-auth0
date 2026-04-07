package com.circulo.auth0.service;

/**
 * Tokens de un solo uso para enlazar el callback OIDC con {@link com.liferay.portal.kernel.security.auto.login.AutoLogin}.
 */
public interface Auth0LoginTokenService {

	/**
	 * Genera y registra un token aleatorio asociado al {@code userId} (TTL corto).
	 */
	String generateToken(long userId);

	/**
	 * Comprueba si el token existe y no ha caducado (sin consumirlo).
	 */
	boolean validateToken(String token);

	/**
	 * Invalida el token y devuelve el {@code userId} si era válido y no estaba caducado; si no, {@code null}.
	 * Uso único: tras consumir, el token no debe volver a aceptarse.
	 */
	Long consumeToken(String token);

}
