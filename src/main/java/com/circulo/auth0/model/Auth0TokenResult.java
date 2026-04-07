package com.circulo.auth0.model;

/**
 * Resultado del intercambio en {@code /oauth/token}.
 */
public class Auth0TokenResult {

	private String _accessToken;
	private String _idToken;
	private String _refreshToken;
	private String _tokenType;
	private long _expiresInSeconds;

	public String getAccessToken() {
		return _accessToken;
	}

	public void setAccessToken(String accessToken) {
		_accessToken = accessToken;
	}

	public String getIdToken() {
		return _idToken;
	}

	public void setIdToken(String idToken) {
		_idToken = idToken;
	}

	public String getRefreshToken() {
		return _refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		_refreshToken = refreshToken;
	}

	public String getTokenType() {
		return _tokenType;
	}

	public void setTokenType(String tokenType) {
		_tokenType = tokenType;
	}

	public long getExpiresInSeconds() {
		return _expiresInSeconds;
	}

	public void setExpiresInSeconds(long expiresInSeconds) {
		_expiresInSeconds = expiresInSeconds;
	}

}
