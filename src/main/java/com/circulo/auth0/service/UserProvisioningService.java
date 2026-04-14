package com.circulo.auth0.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Aprovisionamiento de usuarios Liferay a partir de claims OIDC (altas nuevas; existentes sin
 * cambiar perfil).
 */
public interface UserProvisioningService {

	/**
	 * Crea el usuario local si no existe (por email); si ya existe, devuelve su {@code userId} sin
	 * modificar nombre, apellidos ni demás atributos de perfil.
	 *
	 * @param request petición actual (para companyId, locale, auditoría)
	 * @param subject claim {@code sub}
	 * @param claims claims estándar u opcionales (email, name, etc.)
	 * @return userId Liferay
	 */
	long provisionOrUpdateUser(
			HttpServletRequest request, String subject,
			Map<String, Object> claims)
		throws Exception;

}
