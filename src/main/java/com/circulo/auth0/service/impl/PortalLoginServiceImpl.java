package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.PortalLoginService;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Respaldo opcional para asociar sesión manualmente. El flujo Auth0 principal usa {@code AutoLogin}.
 */
@Component(immediate = true, service = PortalLoginService.class)
public class PortalLoginServiceImpl implements PortalLoginService {

	private static final Log _log = LogFactoryUtil.getLog(PortalLoginServiceImpl.class);

	@Reference
	private UserLocalService _userLocalService;

	@Override
	public void establishPortalSession(
			HttpServletRequest request, HttpServletResponse response,
			long userId)
		throws Exception {

		User user = _userLocalService.getUser(userId);

		_userLocalService.updateLastLogin(userId, request.getRemoteAddr());

		HttpSession session = request.getSession(true);

		session.setAttribute(WebKeys.USER, user);
		session.setAttribute(WebKeys.USER_ID, Long.valueOf(userId));

		request.setAttribute(WebKeys.USER, user);
		request.setAttribute(WebKeys.USER_ID, Long.valueOf(userId));

		_log.debug("Sesión portal asociada al userId=" + userId);
	}

}
