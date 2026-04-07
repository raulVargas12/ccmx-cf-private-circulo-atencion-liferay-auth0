package com.circulo.auth0.service;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.model.OidcUserClaims;

/**
 * Validación de {@code id_token} (firma vía JWKS, issuer, audience, exp, nonce) y extracción de claims.
 */
public interface IdTokenValidator {

	/**
	 * Valida el JWT de identidad (según capacidades actuales del implementador) y devuelve claims de usuario.
	 *
	 * @param configuration dominio Auth0, JWKS, clientId (aud típico del id_token)
	 * @param idToken JWT compacto
	 * @param nonce valor guardado en sesión en el paso /authorize
	 * @return claims estándar para aprovisionamiento
	 * @throws Exception token inválido o no confiable
	 */
	OidcUserClaims validateAndExtractClaims(
			Auth0IntegrationConfiguration configuration, String idToken,
			String nonce)
		throws Exception;

}
