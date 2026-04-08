package com.circulo.auth0.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilidades para cookies del flujo OAuth (state, nonce, PKCE). No usar para tokens.
 * <p>
 * Cuando {@code sameSiteRaw} está en blanco, no se envía atributo {@code SameSite}. Valores
 * admitidos (insensibles a mayúsculas): {@code lax}, {@code strict}, {@code none}. {@code None}
 * fuerza {@code Secure} aunque {@code secure} sea {@code false}.
 */
public final class CookieUtil {

	/** Ruta de visibilidad del cookie (todo el host). */
	public static final String COOKIE_PATH = "/";

	private CookieUtil() {
	}

	/**
	 * Añade un cookie con {@code HttpOnly}, path {@value #COOKIE_PATH}, caducidad y opcionalmente
	 * {@code SameSite} vía cabecera {@code Set-Cookie}.
	 *
	 * @param secure {@code true} en HTTPS (DEV/QA/PROD); {@code false} en local HTTP.
	 * @param sameSiteRaw {@code lax}, {@code strict}, {@code none}, o vacío para omitir SameSite.
	 */
	public static void addCookie(
			HttpServletResponse response, String name, String value, int maxAge,
			boolean secure, String sameSiteRaw) {

		String sameSite = _normalizeSameSite(sameSiteRaw);

		if (sameSite == null) {
			_addCookieLegacy(response, name, value, maxAge, secure);

			return;
		}

		boolean effectiveSecure = secure || "None".equals(sameSite);

		response.addHeader(
			"Set-Cookie",
			_buildSetCookieHeader(name, value, maxAge, effectiveSecure, sameSite));
	}

	/**
	 * Invalida el cookie en el cliente (maxAge 0, mismo path y flags).
	 */
	public static void clearCookie(
			HttpServletResponse response, String name, boolean secure,
			String sameSiteRaw) {

		String sameSite = _normalizeSameSite(sameSiteRaw);

		if (sameSite == null) {
			_clearCookieLegacy(response, name, secure);

			return;
		}

		boolean effectiveSecure = secure || "None".equals(sameSite);

		response.addHeader(
			"Set-Cookie",
			_buildSetCookieHeader(name, "", 0, effectiveSecure, sameSite));
	}

	private static void _addCookieLegacy(
			HttpServletResponse response, String name, String value, int maxAge,
			boolean secure) {

		Cookie cookie = new Cookie(name, value);

		cookie.setHttpOnly(true);
		cookie.setSecure(secure);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(maxAge);

		response.addCookie(cookie);
	}

	private static void _clearCookieLegacy(
			HttpServletResponse response, String name, boolean secure) {

		Cookie cookie = new Cookie(name, "");

		cookie.setHttpOnly(true);
		cookie.setSecure(secure);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(0);

		response.addCookie(cookie);
	}

	private static String _buildSetCookieHeader(
			String name, String value, int maxAge, boolean secure,
			String sameSite) {

		StringBuilder sb = new StringBuilder(128);

		sb.append(name);
		sb.append('=');

		if (value != null) {
			sb.append(_escapeCookieValue(value));
		}

		sb.append("; Path=").append(COOKIE_PATH);
		sb.append("; HttpOnly");
		sb.append("; Max-Age=").append(maxAge);

		if (secure) {
			sb.append("; Secure");
		}

		sb.append("; SameSite=").append(sameSite);

		return sb.toString();
	}

	private static String _escapeCookieValue(String value) {
		if ((value.indexOf(';') < 0) && (value.indexOf('\n') < 0) &&
			(value.indexOf('\r') < 0)) {

			return value;
		}

		StringBuilder sb = new StringBuilder(value.length() + 8);

		sb.append('"');

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);

			if ((c == '"') || (c == '\\')) {
				sb.append('\\');
			}

			sb.append(c);
		}

		sb.append('"');

		return sb.toString();
	}

	/**
	 * @return {@code Lax}, {@code Strict}, {@code None}, o {@code null} si no debe enviarse
	 *     SameSite.
	 */
	private static String _normalizeSameSite(String raw) {
		if ((raw == null) || raw.trim().isEmpty()) {
			return null;
		}

		String s = raw.trim();

		if (s.equalsIgnoreCase("lax")) {
			return "Lax";
		}

		if (s.equalsIgnoreCase("strict")) {
			return "Strict";
		}

		if (s.equalsIgnoreCase("none")) {
			return "None";
		}

		return "Lax";
	}

	/**
	 * Obtiene el valor del primer cookie con el nombre dado, o {@code null}.
	 */
	public static String getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}

}
