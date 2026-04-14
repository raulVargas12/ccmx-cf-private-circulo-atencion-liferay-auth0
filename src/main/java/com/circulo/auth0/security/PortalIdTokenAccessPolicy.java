package com.circulo.auth0.security;

import com.auth0.jwt.interfaces.DecodedJWT;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;

/**
 * Valida claims de aplicación y roles de portal sobre un {@code id_token} ya verificado.
 */
public interface PortalIdTokenAccessPolicy {

	/**
	 * @throws PortalAccessDeniedException si la app requerida o los roles no cumplen la política
	 */
	void assertPortalAccessAllowed(
			Auth0IntegrationConfiguration configuration, DecodedJWT jwt)
		throws PortalAccessDeniedException;

}
