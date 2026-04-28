# Circulo Atención — módulo Auth0 (Liferay 7.3)

## Propósito

Módulo OSGi para Liferay 7.3 con **Auth0** (**Authorization Code + PKCE**): login, callback, validación de `id_token`, aprovisionamiento de usuario, sesión portal vía `AutoLogin`, logout federado y almacenamiento de tokens OAuth en caché de clúster.

## Endpoints (JAX-RS Whiteboard)

La aplicación usa base `osgi.jaxrs.application.base=/auth` (prefijo Liferay `/o`).

| Método | Ruta | Recurso |
|--------|------|---------|
| GET | `/o/auth/login` | `Auth0LoginResource` |
| GET | `/o/auth/callback` | `Auth0CallbackResource` |
| GET | `/o/auth/logout` | `Auth0LogoutResource` |

## Callback (`GET /o/auth/callback`): errores y redirecciones

Las respuestas **303 See Other** envían al navegador primero a **Auth0 `/v2/logout`** (con `client_id` y `returnTo` = URL absoluta de la página del portal), para **cerrar la sesión en Auth0**; Auth0 redirige después a **`{portalUrl}`** + ruta (y query). Así un nuevo `/o/auth/login` vuelve a mostrar el login de credenciales. Si falta `clientId` o configuración, se omite el logout y solo se redirige al portal.

Los fallos del callback que antes respondían **400 / 403 / 500** en texto plano ahora **redirigen** a **`auth0OAuthErrorPagePath`** con **`?code=`** (constantes en `Auth0Constants`), mismos mecanismos de logout y cookies que el resto de errores.

**Importante:** en el dashboard Auth0, **Allowed Logout URLs** debe incluir las URLs absolutas de las páginas de error (con query `?code=…` si aplica), o patrones que Auth0 permita para `returnTo`.

### Auth0 devuelve `error` en la query (antes del intercambio de código)

| Condición | Redirección | Notas |
|-----------|-------------|--------|
| `error=access_denied` y `error_description` (minúsculas) contiene `email_not_verified` | **Logout Auth0** → **`auth0EmailNotVerifiedPagePath`** (defecto `/web/guest/email-no-verificado`) | Sin parámetro `code` en query. |
| `error=access_denied` y **no** es el caso anterior (p. ej. usuario cancela) | **Logout Auth0** → **`auth0OAuthErrorPagePath`** + `?code=access_denied` | Defecto ruta `/web/guest/error-auth`. |
| Cualquier otro `error` de OAuth (p. ej. `server_error`) | **Logout Auth0** → misma página de error OAuth + `?code={error}` | `{error}` es el valor enviado por Auth0 (URL-encoded). |

Si la configuración OSGi no está cargada o no hay `clientId`, en estos ramos solo se redirige al portal (sin pasar por `/v2/logout`).

### Sin `error` de Auth0: validación previa al intercambio de código

| Condición | Comportamiento |
|-----------|----------------|
| Faltan `code` o `state` | **303** → logout Auth0 (si aplica) → **`auth0OAuthErrorPagePath`** + `?code=callback_missing_params` |
| `_configuration` nulo | Igual con `?code=configuration_unavailable` |
| Cookie `AUTH0_STATE` vacía o distinta de `state` (posible CSRF) | Limpieza cookies flujo; **303** → logout Auth0 → `?code=invalid_state` |
| Faltan cookies PKCE / nonce | Limpieza cookies flujo; **303** → logout Auth0 → `?code=pkce_missing` |

### Tras intercambio de código (dentro del flujo de login)

| Condición | Comportamiento |
|-----------|----------------|
| **`PortalAccessDeniedException`** (política app/roles en `id_token`, etc.) | Limpieza de sesión/cookies del flujo; **303** a **logout Auth0** → **`auth0OAuthErrorPagePath`** + `?code=portal_access_denied`. |
| **`IllegalStateException`** / **`IllegalArgumentException`** | Limpieza cookies flujo; **303** → logout Auth0 → `?code=login_process_error` (detalle solo en logs). |
| Otra **`Exception`** | Limpieza cookies flujo; **303** → logout Auth0 → `?code=unexpected_login_error`. |

### Login correcto (referencia)

**302** a `{portalUrl}` + **`postLoginRedirectPath`** (si viene vacío en config se usa `/group/guest/home`).

---

## Configuración OSGi

PID: `com.circulo.auth0.config.Auth0IntegrationConfiguration`  
(System Settings, categoría **circulo-auth0**, o `osgi/configs`).

| Propiedad | Descripción |
|-----------|-------------|
| `auth0Domain` / `auth0CustomDomain` | Host de autorización y token (uno u otro obligatorio). |
| `clientId` / `clientSecret` | Cliente Auth0; secret opcional según tipo de aplicación. |
| `redirectUri` | Callback exacto registrado en Auth0 (`https://…/o/auth/callback`). |
| `logoutReturnUri` | `returnTo` permitido en Auth0 (si vacío se usa URL del portal). |
| `audience` | Obligatorio para el flujo actual (API en Auth0). |
| `scopes` | p. ej. `openid profile email`. |
| `jwksUri` | Opcional; por defecto `/.well-known/jwks.json` del mismo host. |
| `cookiesSecure` | `false` en local HTTP; `true` en DEV/QA/PROD con HTTPS. |
| `cookieSameSite` (`cookie-same-site` en `.config`) | `lax` (defecto), `strict`, `none` o vacío (sin atributo SameSite). `none` fuerza `Secure`. |
| `postLoginRedirectPath` | Ruta relativa tras login OK (defecto `/group/guest/home`). |
| `portalAccessAppClaimUri` | Claim del `id_token` con la app (defecto `https://circulo.com/app`). |
| `portalExpectedApp` | Valor permitido (defecto `portal`). |
| `portalRolesClaimUri` | Claim tipo array de strings con roles (defecto `https://circulo.com/roles`). |
| `portalAllowedRoles` | Roles permitidos, separados por coma (defecto `portal:agent,portal:agent:editor`). |
| `authBridgeDataClaimUri` | Objeto con `usuario`, `nombre`, `apellidos`, `correo` (defecto `https://auth-bridge.com/data`). |
| `auth0OAuthErrorPagePath` | Ruta del portlet React de errores OAuth (defecto `/web/guest/error-auth`); el callback añade `?code=`. |
| `auth0EmailNotVerifiedPagePath` | Ruta cuando Auth0 deniega por email no verificado (defecto `/web/guest/email-no-verificado`). |

Si la app o los roles no cumplen la política del token (`PortalAccessDeniedException`), **no** se completa el login; se limpian cookies del flujo y sesión HTTP si aplica, y el callback **redirige** a **logout Auth0** y luego a **`auth0OAuthErrorPagePath`** con **`?code=portal_access_denied`** (véase la sección *Callback* arriba).

### Perfil Liferay vs claims del token

- **Usuario nuevo:** `given_name` / `family_name` con prioridad; si faltan, `nombre` / `apellidos` del objeto auth-bridge (`authBridgeDataClaimUri`). **Screen name** desde `usuario` del auth-bridge si es único y válido; si no, Liferay lo genera. Email: claim `email` o, si falta, `correo` del auth-bridge.
- **Usuario ya existente (mismo email):** no se actualiza el perfil en Liferay en cada login.
- **No implementados aquí (valorar aparte):** `picture` (foto de perfil Liferay requiere descargar bytes y `updatePortrait`), `email_verified` / cambio de email (política Liferay y verificación), `nickname`, `name`, objetos anidados (`user_info`, `available_apps`, etc.), claims numéricos/booleanos de auth-bridge (solo informativos salvo reglas de negocio futuras).

## Clúster Liferay y sesión HTTP

- **`Auth0LoginTokenService`**, **`UserTokenStore`** y **`SessionTokenStore`** usan **`MultiVMPool`** (cachés `com.circulo.auth0.cluster.*`). Los datos replican entre nodos según la configuración de caché del portal.
- **`SessionTokenStore`** indexa por **`HttpSession.getId()`**. En clúster hace falta **replicación de sesión** en Liferay o **sticky session** en el balanceador para que el mismo `JSESSIONID` (y el mismo id interno de sesión) sea válido en el nodo que atienda cada petición. Sin eso, un usuario podría perder los tokens OAuth tras el login al caer en otro nodo.
- **Operaciones:** coordinar con infraestructura (sticky o replicación documentada en el manual de Liferay para vuestra versión).

## Logs

En PROD conviene dejar el paquete `com.circulo.auth0` en **INFO** o **WARN**; **DEBUG** solo en entornos locales o de diagnóstico temporal. El módulo evita en logs dominios completos de Auth0, URIs JWKS, `sub` en INFO y listas de roles en texto claro; detalle de `userId` queda en DEBUG en el callback.

## Compilación y despliegue

- JDK 8, `release.dxp.api` alineado al servidor (p. ej. `7.3.10.u32`).
- `gradlew deploy` o copiar el JAR al directorio `deploy` del portal.

## Licencia / uso interno

Proyecto Circulo Atención (privado).
