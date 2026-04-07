package com.circulo.auth0.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Proxy HTTP hacia Apigee inyectando autorización (p. ej. Bearer desde sesión).
 */
public interface ApigeeProxyService {

	/**
	 * Reenvía la petición al backend Apigee según path relativo.
	 *
	 * @param request petición cliente
	 * @param response respuesta cliente
	 * @param pathRemainder segmento tras el prefijo del servlet (pathInfo normalizado)
	 */
	void proxy(
			HttpServletRequest request, HttpServletResponse response,
			String pathRemainder)
		throws IOException;

}
