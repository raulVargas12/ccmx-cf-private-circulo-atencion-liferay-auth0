package com.circulo.auth0.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Aprovisionamiento o actualización de usuarios Liferay a partir de claims OIDC.
 * <p>
 * TODO: encapsular {@code UserLocalService} / {@code ServiceContext} según políticas del portal.
 */
public interface UserProvisioningService {

	/**
	 * Crea o actualiza el usuario local asociado al subject/claims.
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
