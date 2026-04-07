package com.circulo.auth0.model;

/**
 * Claims OIDC mínimos extraídos del {@code id_token} tras validación (parcial o completa).
 */
public class OidcUserClaims {

	private String _sub;
	private String _email;
	private String _givenName;
	private String _familyName;

	public String getSub() {
		return _sub;
	}

	public void setSub(String sub) {
		_sub = sub;
	}

	public String getEmail() {
		return _email;
	}

	public void setEmail(String email) {
		_email = email;
	}

	public String getGivenName() {
		return _givenName;
	}

	public void setGivenName(String givenName) {
		_givenName = givenName;
	}

	public String getFamilyName() {
		return _familyName;
	}

	public void setFamilyName(String familyName) {
		_familyName = familyName;
	}

}
