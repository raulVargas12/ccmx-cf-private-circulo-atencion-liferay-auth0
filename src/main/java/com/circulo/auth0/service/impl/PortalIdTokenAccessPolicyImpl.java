package com.circulo.auth0.service.impl;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.security.PortalAccessDeniedException;
import com.circulo.auth0.security.PortalIdTokenAccessPolicy;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

/**
 * Comprueba {@code https://circulo.com/app} y {@code https://circulo.com/roles} (URIs
 * configurables).
 */
@Component(immediate = true, service = PortalIdTokenAccessPolicy.class)
public class PortalIdTokenAccessPolicyImpl implements PortalIdTokenAccessPolicy {

	private static final Log _log = LogFactoryUtil.getLog(
		PortalIdTokenAccessPolicyImpl.class);

	@Override
	public void assertPortalAccessAllowed(
			Auth0IntegrationConfiguration configuration, DecodedJWT jwt)
		throws PortalAccessDeniedException {

		String appClaimUri = _trim(configuration.portalAccessAppClaimUri());
		String expectedApp = _trim(configuration.portalExpectedApp());
		String rolesClaimUri = _trim(configuration.portalRolesClaimUri());
		String allowedRolesRaw = configuration.portalAllowedRoles();

		if (Validator.isBlank(appClaimUri) || Validator.isBlank(expectedApp) ||
			Validator.isBlank(rolesClaimUri) ||
			Validator.isBlank(allowedRolesRaw)) {

			throw new PortalAccessDeniedException(
				"Configuración incompleta: app/roles de portal");
		}

		Claim appClaim = jwt.getClaim(appClaimUri);

		if (appClaim.isNull()) {
			_deny("Claim de aplicación ausente: " + appClaimUri);
		}

		String appValue = appClaim.asString();

		if (Validator.isBlank(appValue)) {
			_deny("Claim de aplicación vacía");
		}

		if (!expectedApp.equalsIgnoreCase(appValue.trim())) {
			_deny(
				"Aplicación no autorizada (esperada=" + expectedApp + ", recibida=" +
					appValue + ")");
		}

		Claim rolesClaim = jwt.getClaim(rolesClaimUri);

		if (rolesClaim.isNull()) {
			_deny("Claim de roles ausente: " + rolesClaimUri);
		}

		List<String> tokenRoles = _rolesAsStringList(rolesClaim);

		if (tokenRoles.isEmpty()) {
			_deny("Sin roles en el token");
		}

		Set<String> allowed = _parseAllowedRoles(allowedRolesRaw);

		if (allowed.isEmpty()) {
			_deny("No hay roles permitidos configurados");
		}

		boolean match = false;

		for (String r : tokenRoles) {
			if (r == null) {
				continue;
			}

			if (allowed.contains(r.trim().toLowerCase(Locale.ROOT))) {
				match = true;

				break;
			}
		}

		if (!match) {
			_deny(
				"Ningún rol del token coincide con los permitidos para portal");
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Portal access OK; app=" + appValue + " roles=" + tokenRoles);
		}
	}

	private static void _deny(String reason) throws PortalAccessDeniedException {
		throw new PortalAccessDeniedException(reason);
	}

	private static List<String> _rolesAsStringList(Claim rolesClaim) {
		try {
			List<String> list = rolesClaim.asList(String.class);

			return (list != null) ? list : Collections.emptyList();
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug("roles claim no es lista de strings: " + e.getMessage());
			}

			return Collections.emptyList();
		}
	}

	private static Set<String> _parseAllowedRoles(String raw) {
		Set<String> out = new HashSet<>();

		if (raw == null) {
			return out;
		}

		for (String part : raw.split(",")) {
			String t = part.trim();

			if (!t.isEmpty()) {
				out.add(t.toLowerCase(Locale.ROOT));
			}
		}

		return out;
	}

	private static String _trim(String s) {
		return (s == null) ? "" : s.trim();
	}

}
