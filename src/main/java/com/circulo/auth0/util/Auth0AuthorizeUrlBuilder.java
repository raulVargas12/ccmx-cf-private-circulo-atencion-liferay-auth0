package com.circulo.auth0.util;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Construye la URL del endpoint {@code /authorize} de Auth0 (Authorization Code + PKCE).
 */
public final class Auth0AuthorizeUrlBuilder {

	private Auth0AuthorizeUrlBuilder() {
	}

	/**
	 * Construye la URL completa de autorización.
	 * <p>
	 * Requiere dominio (custom o tenant), {@code clientId}, {@code redirectUri} y
	 * {@code audience} configurados.
	 */
	public static String buildAuthorizeUrl(
			Auth0IntegrationConfiguration config, String state, String nonce,
			String codeChallenge) {

		validateAuthorizeParameters(config, state, nonce, codeChallenge);

		String host = resolveAuthorizeHost(config);

		StringBuilder query = new StringBuilder(256);

		appendParam(
			query, Auth0Constants.PARAM_RESPONSE_TYPE,
			Auth0Constants.RESPONSE_TYPE_CODE);
		appendParam(query, Auth0Constants.PARAM_CLIENT_ID, config.clientId());
		appendParam(
			query, Auth0Constants.PARAM_REDIRECT_URI, config.redirectUri());
		appendParam(query, Auth0Constants.PARAM_SCOPE, config.scopes());
		appendParam(query, Auth0Constants.PARAM_AUDIENCE, config.audience());
		appendParam(
			query, Auth0Constants.PARAM_CODE_CHALLENGE, codeChallenge);
		appendParam(
			query, Auth0Constants.PARAM_CODE_CHALLENGE_METHOD,
			Auth0Constants.CHALLENGE_METHOD_S256);
		appendParam(query, Auth0Constants.PARAM_STATE, state);
		appendParam(query, Auth0Constants.PARAM_NONCE, nonce);

		return "https://" + host + "/authorize?" + query;
	}

	/**
	 * Host para {@code https://{host}/authorize}: custom domain si viene informado; si no,
	 * {@code auth0Domain}. Se normaliza quitando esquema y barras finales.
	 */
	public static String resolveAuthorizeHost(
			Auth0IntegrationConfiguration config) {

		String custom = trimToEmpty(config.auth0CustomDomain());

		if (!custom.isEmpty()) {
			return normalizeHost(custom);
		}

		String domain = trimToEmpty(config.auth0Domain());

		if (domain.isEmpty()) {
			throw new IllegalStateException(
				"Auth0: debe configurarse auth0CustomDomain o auth0Domain");
		}

		return normalizeHost(domain);
	}

	/**
	 * Valida configuración mínima antes de escribir la sesión (dominio, cliente, redirect, audience, scopes).
	 */
	public static void validateConfigurationForLogin(
			Auth0IntegrationConfiguration config) {

		if (config == null) {
			throw new IllegalStateException("Auth0: configuración no disponible");
		}

		if (isBlank(config.clientId())) {
			throw new IllegalStateException("Auth0: clientId es obligatorio");
		}

		if (isBlank(config.redirectUri())) {
			throw new IllegalStateException("Auth0: redirectUri es obligatorio");
		}

		if (isBlank(config.audience())) {
			throw new IllegalStateException(
				"Auth0: audience es obligatorio para este flujo");
		}

		if (isBlank(config.scopes())) {
			throw new IllegalStateException("Auth0: scopes no puede estar vacío");
		}

		resolveAuthorizeHost(config);
	}

	private static void validateAuthorizeParameters(
			Auth0IntegrationConfiguration config, String state, String nonce,
			String codeChallenge) {

		validateConfigurationForLogin(config);

		if (isBlank(state) || isBlank(nonce) || isBlank(codeChallenge)) {
			throw new IllegalStateException(
				"Auth0: state, nonce y codeChallenge son obligatorios");
		}
	}

	private static String normalizeHost(String value) {
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

	private static void appendParam(
		StringBuilder sb, String name, String value) {

		if (sb.length() > 0) {
			sb.append('&');
		}

		sb.append(urlEncode(name));
		sb.append('=');
		sb.append(urlEncode(value));
	}

	private static String urlEncode(String raw) {
		try {
			return URLEncoder.encode(raw, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF-8 not supported", e);
		}
	}

	private static boolean isBlank(String s) {
		return (s == null) || s.trim().isEmpty();
	}

	private static String trimToEmpty(String s) {
		return (s == null) ? "" : s.trim();
	}

}
