package com.circulo.auth0.security.jwks;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.SigningKeyNotFoundException;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.Validator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Descarga JWKS de Auth0 y cachea claves RSA por {@code kid} con TTL fijo.
 */
public final class Auth0JwksRsaKeyCache {

	private static final long _TTL_MS = 10L * 60L * 1000L;

	private static final long _STALE_FALLBACK_MS = 5L * 60L * 1000L;

	private static final long _NEGATIVE_KID_TTL_MS = 30L * 1000L;

	private static final int _CONNECT_TIMEOUT_MS = 15000;

	private static final int _READ_TIMEOUT_MS = 15000;

	private static final ConcurrentHashMap<String, Bucket> _buckets =
		new ConcurrentHashMap<>();

	private Auth0JwksRsaKeyCache() {
	}

	public static RSAPublicKey getRsaPublicKey(String jwksUrl, String kid)
		throws Exception {

		if (Validator.isBlank(jwksUrl)) {
			throw new IllegalStateException("jwksUri no configurada");
		}

		if (Validator.isBlank(kid)) {
			throw new IllegalStateException("JWT sin kid en header");
		}

		Bucket bucket = _buckets.computeIfAbsent(jwksUrl, k -> new Bucket());

		return bucket.getKey(jwksUrl, kid);
	}

	private static final class Bucket {

		private final ConcurrentHashMap<String, RSAPublicKey> _keysByKid =
			new ConcurrentHashMap<>();

		private final ConcurrentHashMap<String, Long> _negativeKids =
			new ConcurrentHashMap<>();

		private volatile long _loadedAtMillis;

		RSAPublicKey getKey(String jwksUrl, String kid) throws Exception {
			synchronized (this) {
				long now = System.currentTimeMillis();

				boolean cacheEmpty = _keysByKid.isEmpty();
				boolean expired =
					!cacheEmpty && ((now - _loadedAtMillis) >= _TTL_MS);

				RSAPublicKey cached = _keysByKid.get(kid);

				if (!cacheEmpty && !expired && (cached != null)) {
					return cached;
				}

				if (!cacheEmpty && !expired && _isNegativeKidCached(kid, now)) {
					throw new SigningKeyNotFoundException(
						"No hay clave RSA en JWKS para el kid indicado", null);
				}

				try {
					_reload(jwksUrl);
					_loadedAtMillis = System.currentTimeMillis();
				}
				catch (Exception e) {
					if ((cached != null) && !_isHardExpired(now)) {
						return cached;
					}

					throw e;
				}

				cached = _keysByKid.get(kid);

				if (cached == null) {
					_negativeKids.put(kid, now);

					throw new SigningKeyNotFoundException(
						"No hay clave RSA en JWKS para el kid indicado", null);
				}

				_negativeKids.remove(kid);

				return cached;
			}
		}

		private boolean _isHardExpired(long now) {
			if (_loadedAtMillis <= 0) {
				return true;
			}

			return (now - _loadedAtMillis) > (_TTL_MS + _STALE_FALLBACK_MS);
		}

		private boolean _isNegativeKidCached(String kid, long now) {
			Long negativeAt = _negativeKids.get(kid);

			if (negativeAt == null) {
				return false;
			}

			if ((now - negativeAt) <= _NEGATIVE_KID_TTL_MS) {
				return true;
			}

			_negativeKids.remove(kid, negativeAt);

			return false;
		}

		private void _reload(String jwksUrl) throws Exception {
			String body = _httpGet(jwksUrl);

			JSONObject jwks = JSONFactoryUtil.createJSONObject(body);

			JSONArray keys = jwks.getJSONArray("keys");

			if (keys == null) {
				throw new IllegalStateException("JWKS sin array keys");
			}

			_keysByKid.clear();
			_negativeKids.clear();

			for (int i = 0; i < keys.length(); i++) {
				JSONObject keyJson = keys.getJSONObject(i);

				if (keyJson == null) {
					continue;
				}

				Map<String, Object> map = _jsonObjectToMap(keyJson);

				Jwk jwk = Jwk.fromValues(map);

				if (!"RSA".equals(jwk.getType())) {
					continue;
				}

				String jwkKid = jwk.getId();

				if (Validator.isBlank(jwkKid)) {
					continue;
				}

				_keysByKid.put(jwkKid, (RSAPublicKey)jwk.getPublicKey());
			}
		}

		private static String _httpGet(String jwksUrl) throws Exception {
			HttpURLConnection connection = null;

			try {
				URL url = URI.create(jwksUrl).toURL();

				connection = (HttpURLConnection)url.openConnection();

				connection.setConnectTimeout(_CONNECT_TIMEOUT_MS);
				connection.setReadTimeout(_READ_TIMEOUT_MS);
				connection.setRequestMethod("GET");
				connection.setInstanceFollowRedirects(true);

				int code = connection.getResponseCode();

				if ((code < 200) || (code > 299)) {
					throw new IllegalStateException(
						"JWKS HTTP " + code);
				}

				try (InputStream inputStream = connection.getInputStream();
					BufferedReader reader = new BufferedReader(
						new InputStreamReader(
							inputStream, StandardCharsets.UTF_8))) {

					StringBuilder sb = new StringBuilder(4096);
					String line;

					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}

					return sb.toString();
				}
			}
			finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}

		private static Map<String, Object> _jsonObjectToMap(JSONObject obj)
			throws JSONException {

			Map<String, Object> map = new HashMap<>();

			Iterator<String> keys = obj.keys();

			while (keys.hasNext()) {
				String key = keys.next();
				Object value = obj.get(key);

				if (value instanceof JSONObject) {
					map.put(key, _jsonObjectToMap((JSONObject)value));
				}
				else if (value instanceof JSONArray) {
					map.put(key, _jsonArrayToList((JSONArray)value));
				}
				else {
					map.put(key, value);
				}
			}

			return map;
		}

		private static List<Object> _jsonArrayToList(JSONArray arr)
			throws JSONException {

			List<Object> list = new ArrayList<>(arr.length());

			for (int i = 0; i < arr.length(); i++) {
				Object el = arr.get(i);

				if (el instanceof JSONObject) {
					list.add(_jsonObjectToMap((JSONObject)el));
				}
				else if (el instanceof JSONArray) {
					list.add(_jsonArrayToList((JSONArray)el));
				}
				else {
					list.add(el);
				}
			}

			return list;
		}

	}

}
