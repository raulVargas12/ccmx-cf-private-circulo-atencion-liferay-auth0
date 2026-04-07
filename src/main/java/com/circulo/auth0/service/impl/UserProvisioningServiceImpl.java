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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Aprovisiona usuarios locales a partir de claims OIDC (email obligatorio).
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

		String email = GetterUtil.getString((String)claims.get("email"));

		if (Validator.isBlank(email)) {
			throw new IllegalStateException(
				"No se puede aprovisionar usuario sin email");
		}

		User user = _userLocalService.fetchUserByEmailAddress(companyId, email);

		if (user != null) {
			return user.getUserId();
		}

		String firstName = GetterUtil.getString((String)claims.get("given_name"));
		String lastName = GetterUtil.getString((String)claims.get("family_name"));

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			User.class.getName(), request);

		long creatorUserId = _userLocalService.getDefaultUserId(companyId);

		String password1 = com.liferay.portal.kernel.util.PwdGenerator.getPassword();

		user = _userLocalService.addUser(
			creatorUserId, companyId, false, password1, password1, true, null,
			email, 0, "", PortalUtil.getLocale(request), firstName, "", lastName,
			0, 0, true, 1, 1, 1970, "", new long[0], new long[0], new long[0],
			new long[0], false, serviceContext);

		_userLocalService.updateStatus(
			user.getUserId(), WorkflowConstants.STATUS_APPROVED, serviceContext);

		return user.getUserId();
	}

}
