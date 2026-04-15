package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.UserProvisioningService;

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Da de alta usuarios nuevos Liferay a partir de claims OIDC y {@code auth-bridge}. Si el usuario
 * ya existe (mismo email), no modifica su perfil.
 */
@Component(immediate = true, service = UserProvisioningService.class)
public class UserProvisioningServiceImpl implements UserProvisioningService {

	@Reference
	private UserLocalService _userLocalService;

	@Override
	public long provisionOrUpdateUser(
			HttpServletRequest request, String subject,
			Map<String, Object> claims)
		throws Exception {

		long companyId = PortalUtil.getCompanyId(request);

		String email = _emailFromClaims(claims);

		if (Validator.isBlank(email)) {
			throw new IllegalStateException(
				"No se puede aprovisionar usuario sin email");
		}

		User user = _userLocalService.fetchUserByEmailAddress(companyId, email);

		if (user != null) {
			return user.getUserId();
		}

		String firstName = _firstNameFromClaims(claims);
		String lastName = _lastNameFromClaims(claims);
		String screenName = _screenNameForNewUser(companyId, claims, email);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			User.class.getName(), request);

		long creatorUserId = _userLocalService.getDefaultUserId(companyId);

		String password1 = com.liferay.portal.kernel.util.PwdGenerator.getPassword();

		// addUser 7.3 (sin OpenID): el 6.º boolean es autoScreenName. passwordReset lo impone la política; SSO lo limpia abajo.
		user = _userLocalService.addUser(
			creatorUserId, companyId, false, password1, password1, false,
			screenName, email, PortalUtil.getLocale(request), firstName, "",
			lastName, 0, 0, true, 1, 1, 1970, "", new long[0], new long[0],
			new long[0], new long[0], false, serviceContext);

		_userLocalService.updateStatus(
			user.getUserId(), WorkflowConstants.STATUS_APPROVED, serviceContext);
		
		//se salta el reset de cotraseña y se marca como verificado el email para no pedirlas en Liferay durante el alta
		_userLocalService.updatePasswordReset(user.getUserId(), false);
		_userLocalService.updateEmailAddressVerified(user.getUserId(), true);
		return user.getUserId();
	}

	private static String _emailFromClaims(Map<String, Object> claims) {
		String email = GetterUtil.getString((String)claims.get("email"));

		if (!Validator.isBlank(email)) {
			return email.trim();
		}

		return GetterUtil.getString((String)claims.get("auth_bridge_correo"));
	}

	private static String _firstNameFromClaims(Map<String, Object> claims) {
		String v = GetterUtil.getString((String)claims.get("given_name"));

		if (!Validator.isBlank(v)) {
			return v.trim();
		}

		return GetterUtil.getString((String)claims.get("auth_bridge_nombre"));
	}

	private static String _lastNameFromClaims(Map<String, Object> claims) {
		String v = GetterUtil.getString((String)claims.get("family_name"));

		if (!Validator.isBlank(v)) {
			return v.trim();
		}

		return GetterUtil.getString((String)claims.get("auth_bridge_apellidos"));
	}

	private String _screenNameForNewUser(
			long companyId, Map<String, Object> claims, String email) {

		String raw = GetterUtil.getString((String)claims.get("auth_bridge_usuario"));

		if (Validator.isBlank(raw)) {
			return null;
		}

		String normalized = _normalizeScreenName(raw);

		if (Validator.isBlank(normalized)) {
			return null;
		}

		try {
			User existing = _userLocalService.fetchUserByScreenName(
				companyId, normalized);

			if ((existing != null) &&
				!email.equalsIgnoreCase(existing.getEmailAddress())) {

				return null;
			}

			return normalized;
		}
		catch (Exception e) {
			return null;
		}
	}

	private static String _normalizeScreenName(String raw) {
		String s = raw.trim().toLowerCase(Locale.ROOT);

		s = s.replaceAll("[^a-z0-9._-]+", "");

		return s.isEmpty() ? null : s;
	}

}
