package com.circulo.auth0.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.model.OidcUserClaims;
import com.circulo.auth0.security.PortalIdTokenAccessPolicy;
import com.circulo.auth0.security.jwks.Auth0JwksRsaKeyCache;
import com.circulo.auth0.service.IdTokenValidator;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Valida {@code id_token} OIDC: firma RS256 vía JWKS, {@code iss}, {@code aud}, {@code exp},
 * {@code nonce}.
 */
@Component(immediate = true, service = IdTokenValidator.class)
public class IdTokenValidatorImpl implements IdTokenValidator {

	private static final Log _log = LogFactoryUtil.getLog(IdTokenValidatorImpl.class);

	private static final long _EXP_LEEWAY_SECONDS = 120L;

	@Reference
	private PortalIdTokenAccessPolicy _portalIdTokenAccessPolicy;

	@Override
	public OidcUserClaims validateAndExtractClaims(
			Auth0IntegrationConfiguration configuration, String idToken,
			String nonce)
		throws Exception {

		if (Validator.isBlank(idToken)) {
			throw new IllegalStateException("id_token ausente");
		}

		if (Validator.isBlank(nonce)) {
			throw new IllegalStateException("nonce de sesión ausente");
		}

		String clientId = configuration.clientId();

		if (Validator.isBlank(clientId)) {
			throw new IllegalStateException("clientId no configurado");
		}

		String resolvedDomain;

		if ((configuration.auth0CustomDomain() != null) &&
			!configuration.auth0CustomDomain().isEmpty()) {

			resolvedDomain = configuration.auth0CustomDomain();
		}
		else {
			resolvedDomain = configuration.auth0Domain();
		}

		if (Validator.isBlank(resolvedDomain)) {
			throw new IllegalStateException(
				"Auth0: debe configurarse auth0CustomDomain o auth0Domain");
		}

		resolvedDomain = _normalizeHost(resolvedDomain);

		String resolvedJwksUri =
			"https://" + resolvedDomain + "/.well-known/jwks.json";

		String configuredJwksUri = configuration.jwksUri();

		final String jwksUriForKeys =
			!Validator.isBlank(configuredJwksUri) ?
				configuredJwksUri.trim() : resolvedJwksUri;

		DecodedJWT headerProbe = JWT.decode(idToken);

		if (Validator.isBlank(headerProbe.getKeyId())) {
			throw new IllegalStateException("JWT sin kid en header");
		}

		if (!"RS256".equals(headerProbe.getAlgorithm())) {
			throw new IllegalStateException(
				"Algoritmo del id_token no permitido (se espera RS256)");
		}

		RSAKeyProvider rsaKeyProvider = new RSAKeyProvider() {

			@Override
			public RSAPublicKey getPublicKeyById(String keyId) {
				try {
					return Auth0JwksRsaKeyCache.getRsaPublicKey(
						jwksUriForKeys, keyId);
				}
				catch (Exception e) {
					throw new IllegalStateException(
						"Error al resolver clave JWKS", e);
				}
			}

			@Override
			public RSAPrivateKey getPrivateKey() {
				return null;
			}

			@Override
			public String getPrivateKeyId() {
				return null;
			}

		};

		Algorithm algorithm = Algorithm.RSA256(rsaKeyProvider);

		JWTVerifier verifier = JWT.require(algorithm)
			.withAudience(clientId)
			.withClaim("nonce", nonce)
			.acceptLeeway(_EXP_LEEWAY_SECONDS)
			.build();

		DecodedJWT jwt;

		try {
			jwt = verifier.verify(idToken);
		}
		catch (JWTVerificationException e) {
			throw new IllegalStateException(
				"id_token inválido: firma, audiencia, expiración o nonce incorrectos",
				e);
		}

		String tokenIssuer = jwt.getIssuer();

		boolean issuerValid = _issuerMatchesExpected(
			resolvedDomain, tokenIssuer);

		if (!issuerValid) {
			throw new RuntimeException("Issuer inválido");
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"id_token: firma RS256, audience, nonce e issuer verificados " +
					"(sin dominios ni URIs en log)");
		}

		_portalIdTokenAccessPolicy.assertPortalAccessAllowed(configuration, jwt);

		OidcUserClaims claims = new OidcUserClaims();

		claims.setSub(jwt.getSubject());
		claims.setEmail(_claimString(jwt, "email"));
		claims.setGivenName(_claimString(jwt, "given_name"));
		claims.setFamilyName(_claimString(jwt, "family_name"));

		_applyAuthBridgeClaims(configuration, jwt, claims);

		if (Validator.isBlank(claims.getSub())) {
			throw new IllegalStateException("id_token sin claim sub");
		}

		String email = claims.getEmail();

		if (Validator.isBlank(email)) {
			email = claims.getAuthBridgeCorreo();
			claims.setEmail(email);
		}

		if (Validator.isBlank(claims.getEmail())) {
			throw new IllegalStateException("id_token sin email ni correo en auth-bridge");
		}

		return claims;
	}

	private static void _applyAuthBridgeClaims(
			Auth0IntegrationConfiguration configuration, DecodedJWT jwt,
			OidcUserClaims claims) {

		String dataUri = configuration.authBridgeDataClaimUri();

		if (Validator.isBlank(dataUri)) {
			return;
		}

		Map<String, Object> data = _claimAsObjectMap(jwt, dataUri.trim());

		if (data == null) {
			return;
		}

		claims.setAuthBridgeUsuario(_mapString(data, "usuario"));
		claims.setAuthBridgeNombre(_mapString(data, "nombre"));
		claims.setAuthBridgeApellidos(_mapString(data, "apellidos"));
		claims.setAuthBridgeCorreo(_mapString(data, "correo"));
	}

	private static Map<String, Object> _claimAsObjectMap(
			DecodedJWT jwt, String claimName) {

		Claim claim = jwt.getClaim(claimName);

		if ((claim == null) || claim.isNull()) {
			return null;
		}

		try {
			return claim.asMap();
		}
		catch (Exception e) {
			Log log = LogFactoryUtil.getLog(IdTokenValidatorImpl.class);

			if (log.isDebugEnabled()) {
				log.debug(
					"No se pudo leer claim como mapa (URI omitida): " +
						e.getMessage());
			}

			return null;
		}
	}

	private static String _mapString(Map<String, Object> map, String key) {
		Object v = map.get(key);

		if (v == null) {
			return null;
		}

		String s = String.valueOf(v).trim();

		return s.isEmpty() ? null : s;
	}

	private static boolean _issuerMatchesExpected(
		String resolvedDomain, String tokenIssuer) {

		if (Validator.isBlank(tokenIssuer)) {
			return false;
		}

		String expected = "https://" + resolvedDomain + "/";

		return _normalizeIssuerForCompare(expected).equals(
			_normalizeIssuerForCompare(tokenIssuer));
	}

	/**
	 * Compara issuers OIDC raíz ({@code https://host} vs {@code https://host/}) de forma estable.
	 */
	private static String _normalizeIssuerForCompare(String issuer) {
		if (Validator.isBlank(issuer)) {
			return "";
		}

		String v = issuer.trim();

		while (v.endsWith("/")) {
			v = v.substring(0, v.length() - 1);
		}

		return v;
	}

	private static String _normalizeHost(String value) {
		String v = value.trim();

		if (v.startsWith("https://")) {
			v = v.substring("https://".length());
		}
		else if (v.startsWith("http://")) {
			v = v.substring("http://".length());
		}

		int slash = v.indexOf('/');

		if (slash >= 0) {
			v = v.substring(0, slash);
		}

		return v;
	}

	private static String _claimString(DecodedJWT jwt, String name) {
		if (jwt.getClaim(name).isNull()) {
			return null;
		}

		return jwt.getClaim(name).asString();
	}

}
