package com.circulo.auth0.config;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * Configuración central del módulo (System Settings / fichero {@code .config} en {@code osgi/configs}).
 */
@ExtendedObjectClassDefinition(
	category = "circulo-auth0",
	scope = ExtendedObjectClassDefinition.Scope.SYSTEM
)
@Meta.OCD(
	id = "com.circulo.auth0.config.Auth0IntegrationConfiguration",
	localization = "content/Language",
	name = "auth0-integration-configuration-name"
)
public interface Auth0IntegrationConfiguration {

	String PID = "com.circulo.auth0.config.Auth0IntegrationConfiguration";

	@Meta.AD(deflt = "", name = "auth0-domain", required = false)
	String auth0Domain();

	@Meta.AD(deflt = "", name = "auth0-custom-domain", required = false)
	String auth0CustomDomain();

	@Meta.AD(deflt = "", name = "client-id", required = false)
	String clientId();

	@Meta.AD(deflt = "", name = "client-secret", required = false)
	String clientSecret();

	@Meta.AD(deflt = "", name = "redirect-uri", required = false)
	String redirectUri();

	@Meta.AD(deflt = "", name = "logout-return-uri", required = false)
	String logoutReturnUri();

	@Meta.AD(deflt = "", name = "audience", required = false)
	String audience();

	@Meta.AD(
		deflt = "openid profile email",
		name = "scopes",
		required = false
	)
	String scopes();

	@Meta.AD(deflt = "", name = "jwks-uri", required = false)
	String jwksUri();

	@Meta.AD(
		deflt = "false",
		description = "cookies-secure-help",
		name = "cookies-secure",
		required = false
	)
	boolean cookiesSecure();

	@Meta.AD(
		deflt = "/group/guest/home",
		description = "post-login-redirect-path-help",
		name = "post-login-redirect-path",
		required = false
	)
	String postLoginRedirectPath();

	@Meta.AD(
		deflt = "lax",
		description = "cookie-same-site-help",
		name = "cookie-same-site",
		required = false
	)
	String cookieSameSite();

	@Meta.AD(
		deflt = "https://circulo.com/app",
		description = "portal-access-app-claim-uri-help",
		name = "portal-access-app-claim-uri",
		required = false
	)
	String portalAccessAppClaimUri();

	@Meta.AD(
		deflt = "portal",
		description = "portal-expected-app-help",
		name = "portal-expected-app",
		required = false
	)
	String portalExpectedApp();

	@Meta.AD(
		deflt = "https://circulo.com/roles",
		description = "portal-roles-claim-uri-help",
		name = "portal-roles-claim-uri",
		required = false
	)
	String portalRolesClaimUri();

	@Meta.AD(
		deflt = "portal:agent,portal:agent:editor",
		description = "portal-allowed-roles-help",
		name = "portal-allowed-roles",
		required = false
	)
	String portalAllowedRoles();

	@Meta.AD(
		deflt = "https://auth-bridge.com/data",
		description = "auth-bridge-data-claim-uri-help",
		name = "auth-bridge-data-claim-uri",
		required = false
	)
	String authBridgeDataClaimUri();

	@Meta.AD(
		deflt = "/web/guest/error-auth",
		description = "auth0-oauth-error-page-path-help",
		name = "auth0-oauth-error-page-path",
		required = false
	)
	String auth0OAuthErrorPagePath();

	@Meta.AD(
		deflt = "/web/guest/email-no-verificado",
		description = "auth0-email-not-verified-page-path-help",
		name = "auth0-email-not-verified-page-path",
		required = false
	)
	String auth0EmailNotVerifiedPagePath();

}
