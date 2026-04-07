package com.circulo.auth0.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Establece la sesión autenticada en el portal Liferay tras un login externo exitoso.
 * <p>
 * TODO: validar el mecanismo soportado en 7.3 (p. ej. {@code PortalUtil#loginRequest},
 * {@code AutoLogin}, hooks de seguridad, 2FA, etc.) sin asumir APIs inexistentes.
 */
public interface PortalLoginService {

	/**
	 * Autentica al usuario en el contexto del portal y asocia la sesión HTTP.
	 *
	 * @param request petición entrante
	 * @param response respuesta (cookies, redirect post-login si aplica)
	 * @param userId usuario Liferay ya aprovisionado
	 */
	void establishPortalSession(
			HttpServletRequest request, HttpServletResponse response,
			long userId)
		throws Exception;

}
