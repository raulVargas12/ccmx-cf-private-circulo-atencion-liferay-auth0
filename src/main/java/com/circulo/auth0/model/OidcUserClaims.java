package com.circulo.auth0.model;

/**
 * Claims OIDC mínimos extraídos del {@code id_token} tras validación (parcial o completa).
 */
public class OidcUserClaims {

	private String _sub;
	private String _email;
	private String _givenName;
	private String _familyName;

	private String _authBridgeUsuario;
	private String _authBridgeNombre;
	private String _authBridgeApellidos;
	private String _authBridgeCorreo;

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

	public String getAuthBridgeUsuario() {
		return _authBridgeUsuario;
	}

	public void setAuthBridgeUsuario(String authBridgeUsuario) {
		_authBridgeUsuario = authBridgeUsuario;
	}

	public String getAuthBridgeNombre() {
		return _authBridgeNombre;
	}

	public void setAuthBridgeNombre(String authBridgeNombre) {
		_authBridgeNombre = authBridgeNombre;
	}

	public String getAuthBridgeApellidos() {
		return _authBridgeApellidos;
	}

	public void setAuthBridgeApellidos(String authBridgeApellidos) {
		_authBridgeApellidos = authBridgeApellidos;
	}

	public String getAuthBridgeCorreo() {
		return _authBridgeCorreo;
	}

	public void setAuthBridgeCorreo(String authBridgeCorreo) {
		_authBridgeCorreo = authBridgeCorreo;
	}

}
