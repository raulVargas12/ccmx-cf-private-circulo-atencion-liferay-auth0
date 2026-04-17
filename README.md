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
| `portalAccessDeniedReturnUri` | `returnTo` del `/v2/logout` de Auth0 si se deniega el acceso; vacío = URL pública del portal. |

Si la app o los roles no cumplen la política, **no** se crea usuario ni sesión OAuth en caché; se limpian cookies del flujo, se invalida la sesión HTTP si existía y se redirige a **logout federado** en Auth0.

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
