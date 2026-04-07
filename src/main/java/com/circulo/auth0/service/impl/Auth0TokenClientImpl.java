package com.circulo.auth0.service.impl;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.model.Auth0TokenResult;
import com.circulo.auth0.service.Auth0TokenClient;
import com.circulo.auth0.util.Auth0OAuthUrls;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.osgi.service.component.annotations.Component;

/**
 * Cliente {@code POST /oauth/token} con cuerpo JSON (PKCE).
 */
@Component(immediate = true, service = Auth0TokenClient.class)
public class Auth0TokenClientImpl implements Auth0TokenClient {

	private static final Log _log = LogFactoryUtil.getLog(Auth0TokenClientImpl.class);

	@Override
	public Auth0TokenResult exchangeAuthorizationCode(
			Auth0IntegrationConfiguration configuration, String code,
			String codeVerifier, String redirectUri)
		throws Exception {

		String tokenUrl = Auth0OAuthUrls.buildTokenEndpointUrl(configuration);

		JSONObject body = JSONFactoryUtil.createJSONObject();

		body.put("grant_type", "authorization_code");
		body.put("client_id", configuration.clientId());
		body.put("code_verifier", codeVerifier);
		body.put("code", code);
		body.put("redirect_uri", redirectUri);

		if (!Validator.isBlank(configuration.clientSecret())) {
			body.put("client_secret", configuration.clientSecret());
		}

		byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

		HttpURLConnection connection = (HttpURLConnection)new URL(
			tokenUrl).openConnection();

		connection.setConnectTimeout(15000);
		connection.setReadTimeout(30000);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);

		try (OutputStream os = connection.getOutputStream()) {
			os.write(payload);
		}

		int status = connection.getResponseCode();

		String responseBody;

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(
					(status >= 400) ? connection.getErrorStream()
						: connection.getInputStream(),
					StandardCharsets.UTF_8))) {

			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			responseBody = sb.toString();
		}
		finally {
			connection.disconnect();
		}

		if (status < 200 || status > 299) {
			_log.error(
				"Auth0 token endpoint respondió HTTP " + status +
					" (cuerpo omitido en log por datos sensibles)");

			throw new IllegalStateException(
				"No se pudo intercambiar el código por tokens (HTTP " + status +
					")");
		}

		JSONObject json = JSONFactoryUtil.createJSONObject(responseBody);

		if (json.has("error")) {
			String err = json.getString("error");

			_log.error(
				"Auth0 token error=" + err + " (descripción omitida en log)");

			throw new IllegalStateException(
				"Auth0 rechazó el intercambio: " + err);
		}

		Auth0TokenResult result = new Auth0TokenResult();

		result.setAccessToken(json.getString("access_token"));
		result.setIdToken(
			json.has("id_token") ? json.getString("id_token") : null);
		result.setRefreshToken(
			json.has("refresh_token") ? json.getString("refresh_token") : null);
		result.setTokenType(
			json.has("token_type") ? json.getString("token_type") : "Bearer");
		result.setExpiresInSeconds(
			json.has("expires_in") ? GetterUtil.getLong(json.get("expires_in")) : 0L);

		return result;
	}

}
