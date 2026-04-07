package com.circulo.auth0.util;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.liferay.portal.kernel.util.Validator;

/**
 * URLs OAuth2 de Auth0 derivadas de la configuración (mismo host que /authorize).
 */
public final class Auth0OAuthUrls {

	private Auth0OAuthUrls() {
	}

	public static String buildTokenEndpointUrl(
			Auth0IntegrationConfiguration configuration) {

		String host = Auth0AuthorizeUrlBuilder.resolveAuthorizeHost(configuration);

		return "https://" + host + "/oauth/token";
	}

	/**
	 * URL del logout federado Auth0 ({@code /v2/logout}) con {@code client_id} y {@code returnTo}.
	 *
	 * @param returnTo URL absoluta permitida en la aplicación Auth0 (Allowed Logout URLs)
	 */
	public static String buildV2LogoutUrl(
			Auth0IntegrationConfiguration configuration, String returnTo) {

		if (Validator.isBlank(configuration.clientId())) {
			throw new IllegalStateException("Auth0: clientId es obligatorio para logout");
		}

		if (Validator.isBlank(returnTo)) {
			throw new IllegalStateException(
				"Auth0: returnTo es obligatorio para logout federado");
		}

		String host = Auth0AuthorizeUrlBuilder.resolveAuthorizeHost(configuration);

		StringBuilder query = new StringBuilder(128);

		appendParam(query, "client_id", configuration.clientId());
		appendParam(query, "returnTo", returnTo);

		return "https://" + host + "/v2/logout?" + query;
	}

	private static void appendParam(StringBuilder sb, String name, String value) {
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

}
