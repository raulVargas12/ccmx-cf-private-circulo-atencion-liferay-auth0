package com.circulo.auth0.constants;

/**
 * Rutas Whiteboard, cookies OAuth/PKCE y nombres de par?metros de autorizaci?n.
 */
public final class Auth0Constants {

	private Auth0Constants() {
	}

	/**
	 * Ruta JAX-RS del login (Liferay expone la app en {@code /o} + base {@code /auth}).
	 */
	public static final String JAX_RS_LOGIN_RELATIVE_PATH = "/o/auth/login";

	/** Patr?n Whiteboard del callback OAuth. */
	public static final String SERVLET_PATTERN_CALLBACK = "/o/auth/callback";

	/** Patr?n Whiteboard de cierre de sesi?n (portal + federado seg?n fases posteriores). */
	public static final String SERVLET_PATTERN_LOGOUT = "/o/auth/logout";

	/** Caducidad de cookies OAuth (state, nonce, code_verifier): 5 minutos. */
	public static final int OAUTH_FLOW_COOKIE_MAX_AGE_SECONDS = 300;

	// --- Nombres de cookies HTTP-only (flujo login / callback; no tokens) ---

	public static final String AUTH0_STATE = "AUTH0_STATE";

	public static final String AUTH0_NONCE = "AUTH0_NONCE";

	public static final String AUTH0_CODE_VERIFIER = "AUTH0_CODE_VERIFIER";

	/**
	 * Cookie de puente hacia {@code AutoLogin} (un solo uso; no es el access_token de Auth0).
	 */
	public static final String AUTH0_LOGIN_TOKEN = "AUTH0_LOGIN_TOKEN";

	/** Caducidad del cookie {@link #AUTH0_LOGIN_TOKEN} (segundos). */
	public static final int AUTH0_LOGIN_TOKEN_MAX_AGE_SECONDS = 120;

	/** Access token API (sesi?n servidor). */
	public static final String AUTH0_ACCESS_TOKEN = "AUTH0_ACCESS_TOKEN";

	/** ID token (sesi?n servidor; no exponer al cliente). */
	public static final String AUTH0_ID_TOKEN = "AUTH0_ID_TOKEN";

	/** Refresh token si Auth0 lo devolvi?. */
	public static final String AUTH0_REFRESH_TOKEN = "AUTH0_REFRESH_TOKEN";

	/** Epoch segundos: caducidad aproximada del access_token. */
	public static final String AUTH0_ACCESS_TOKEN_EXPIRES_AT =
		"AUTH0_ACCESS_TOKEN_EXPIRES_AT";

	// --- Par?metros query /authorize (OAuth2 + OIDC + PKCE) ---

	public static final String PARAM_RESPONSE_TYPE = "response_type";

	public static final String PARAM_CLIENT_ID = "client_id";

	public static final String PARAM_REDIRECT_URI = "redirect_uri";

	public static final String PARAM_SCOPE = "scope";

	public static final String PARAM_AUDIENCE = "audience";

	public static final String PARAM_CODE_CHALLENGE = "code_challenge";

	public static final String PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method";

	public static final String PARAM_STATE = "state";

	public static final String PARAM_NONCE = "nonce";

	public static final String CHALLENGE_METHOD_S256 = "S256";

	public static final String RESPONSE_TYPE_CODE = "code";

	// --- Query {@code code} en página error-auth tras fallos del callback ---

	/** Faltan {@code code} o {@code state} en la vuelta del IdP. */
	public static final String CALLBACK_ERROR_MISSING_PARAMS =
		"callback_missing_params";

	/** Configuración OSGi del módulo no disponible. */
	public static final String CALLBACK_ERROR_CONFIGURATION_UNAVAILABLE =
		"configuration_unavailable";

	/** Cookie {@code AUTH0_STATE} ausente o no coincide con {@code state} (CSRF). */
	public static final String CALLBACK_ERROR_INVALID_STATE = "invalid_state";

	/** Faltan cookies PKCE o nonce del flujo. */
	public static final String CALLBACK_ERROR_PKCE_MISSING = "pkce_missing";

	/** Fallo de intercambio de código, id_token, validación o aprovisionamiento. */
	public static final String CALLBACK_ERROR_LOGIN_PROCESS = "login_process_error";

	/** Cualquier otro error no clasificado. */
	public static final String CALLBACK_ERROR_UNEXPECTED = "unexpected_login_error";

}
