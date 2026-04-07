package com.circulo.auth0.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilidades para cookies del flujo OAuth (state, nonce, PKCE). No usar para tokens.
 */
public final class CookieUtil {

	/** Ruta de visibilidad del cookie (todo el host). */
	public static final String COOKIE_PATH = "/";

	/**
	 * En producción con HTTPS, establecer {@code true} para evitar envío por canal no cifrado.
	 */
	public static final boolean COOKIE_SECURE_DEFAULT = false;

	private CookieUtil() {
	}

	/**
	 * Añade un cookie con {@code HttpOnly}, path {@value #COOKIE_PATH} y caducidad relativa.
	 */
	public static void addCookie(
			HttpServletResponse response, String name, String value, int maxAge) {

		Cookie cookie = new Cookie(name, value);

		cookie.setHttpOnly(true);
		cookie.setSecure(COOKIE_SECURE_DEFAULT);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(maxAge);

		response.addCookie(cookie);
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

	/**
	 * Invalida el cookie en el cliente (maxAge 0, mismo path y flags).
	 */
	public static void clearCookie(HttpServletResponse response, String name) {
		Cookie cookie = new Cookie(name, "");

		cookie.setHttpOnly(true);
		cookie.setSecure(COOKIE_SECURE_DEFAULT);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(0);

		response.addCookie(cookie);
	}

}
