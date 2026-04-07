package com.circulo.auth0.service;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.model.Auth0TokenResult;

/**
 * Cliente HTTP hacia el endpoint {@code /oauth/token} de Auth0 (intercambio de código + PKCE).
 */
public interface Auth0TokenClient {

	/**
	 * Intercambia el authorization code por tokens.
	 *
	 * @param configuration configuración efectiva (issuer, client, secret, etc.)
	 * @param code código devuelto en callback
	 * @param codeVerifier verifier PKCE almacenado en sesión
	 * @param redirectUri URI de callback registrada (debe coincidir con la petición inicial)
	 * @return tokens parseados
	 * @throws Exception error de red, OAuth o parsing; acotar tipos en fase posterior
	 */
	Auth0TokenResult exchangeAuthorizationCode(
			Auth0IntegrationConfiguration configuration, String code,
			String codeVerifier, String redirectUri)
		throws Exception;

}
