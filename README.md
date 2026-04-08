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

## Clúster Liferay y sesión HTTP

- **`Auth0LoginTokenService`**, **`UserTokenStore`** y **`SessionTokenStore`** usan **`MultiVMPool`** (cachés `com.circulo.auth0.cluster.*`). Los datos replican entre nodos según la configuración de caché del portal.
- **`SessionTokenStore`** indexa por **`HttpSession.getId()`**. En clúster hace falta **replicación de sesión** en Liferay o **sticky session** en el balanceador para que el mismo `JSESSIONID` (y el mismo id interno de sesión) sea válido en el nodo que atienda cada petición. Sin eso, un usuario podría perder los tokens OAuth tras el login al caer en otro nodo.
- **Operaciones:** coordinar con infraestructura (sticky o replicación documentada en el manual de Liferay para vuestra versión).

## Compilación y despliegue

- JDK 8, `release.dxp.api` alineado al servidor (p. ej. `7.3.10.u32`).
- `gradlew deploy` o copiar el JAR al directorio `deploy` del portal.

## Licencia / uso interno

Proyecto Circulo Atención (privado).
