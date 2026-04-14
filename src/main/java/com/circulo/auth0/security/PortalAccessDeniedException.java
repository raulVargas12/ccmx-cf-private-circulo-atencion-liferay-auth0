package com.circulo.auth0.security;

/**
 * El {@code id_token} es válido pero el usuario no cumple reglas de aplicación/roles de portal.
 */
public class PortalAccessDeniedException extends RuntimeException {

	public PortalAccessDeniedException(String message) {
		super(message);
	}

}
