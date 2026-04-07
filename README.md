# Circulo Atención — módulo Auth0 (Liferay 7.3)

## Propósito

Módulo OSGi backend para Liferay 7.3 que integrará autenticación manual con **Auth0** mediante **Authorization Code con PKCE**, sin usar el conector OIDC nativo del portal. En esta fase solo existe la **estructura**, **configuración** y **firmas** de las piezas; la lógica de login, callback, validación de tokens, aprovisionamiento de usuario, sesión en el portal y proxy HTTP se implementará después.

## Decisión técnica: exposición de endpoints

- **Login Auth0 (`/o/auth0/login`)**: **OSGi JAX-RS Whiteboard** (`javax.ws.rs.core.Application` + recurso `@Path`), compatible con Liferay 7.3 y 7.4.
- **Callback, logout y proxy Apigee**: siguen en **HTTP Whiteboard (servlets)** por control fino de request/response y proxy.

El inicio de sesión usa `@Context HttpServletRequest`, `Response.Status.FOUND` (302) y la misma lógica de PKCE/sesión (`Auth0LoginRedirectHelper`, `PKCEUtil`, `Auth0AuthorizeUrlBuilder`).

## Estructura de paquetes

| Paquete | Contenido |
|--------|-----------|
| `com.circulo.auth0.config` | Metatype OSGi (`Auth0IntegrationConfiguration`) y constantes de PID. |
| `com.circulo.auth0.constants` | Rutas públicas, nombres de sesión, claves internas. |
| `com.circulo.auth0.jaxrs` | Aplicación JAX-RS (`Auth0Application`) y `Auth0LoginResource`. |
| `com.circulo.auth0.web` | Servlets: callback, logout, proxy; helper de login (`Auth0LoginRedirectHelper`). |
| `com.circulo.auth0.service` | Contratos: token, validación id_token, sesión de tokens, login portal, usuarios, proxy. |
| `com.circulo.auth0.service.impl` | Implementaciones esqueleto (DS), listas para cablear Liferay/APIs en fases posteriores. |
| `com.circulo.auth0.util` | PKCE y construcción de URL de autorización. |
| `com.circulo.auth0.model` | Modelos mínimos compartidos (p. ej. resultado de token). |

## Configuración OSGi

PID: `com.circulo.auth0.config.Auth0IntegrationConfiguration`

| Propiedad | Descripción |
|-----------|-------------|
| `auth0Domain` | Dominio tenant Auth0 (`dev-xxx.auth0.com`). |
| `auth0CustomDomain` | Dominio custom opcional (vacío si no aplica). |
| `clientId` | Client ID de la aplicación Auth0. |
| `clientSecret` | Client secret (si el flujo lo requiere en servidor; validar vs PKCE público). |
| `redirectUri` | URI de callback registrada en Auth0 (debe coincidir con el servlet callback). |
| `logoutReturnUri` | URL post-logout (Auth0 `returnTo` / parámetro equivalente). |
| `audience` | API Audience opcional. |
| `scopes` | Scopes OIDC/OAuth (p. ej. `openid profile email`). |
| `jwksUri` | JWKS para validación de `id_token` (o derivado del issuer; confirmar en implementación). |
| `apigeeBaseUrl` | Base URL del API gateway (proxy). |

Tras desplegar, configurar en **Control Panel → Configuration → System Settings** (categoría definida en el metatype) o vía fichero `.config` en `osgi/configs`.

## Endpoints planeados (Whiteboard)

| Método | Ruta | Servlet |
|--------|------|---------|
| GET | `/o/auth0/login` | `Auth0LoginResource` (JAX-RS Whiteboard) |
| GET | `/o/auth0/callback` | `Auth0CallbackServlet` |
| GET | `/o/auth0/logout` | `Auth0LogoutServlet` |
| * | `/o/proxy/apigee/*` | `ApigeeProxyServlet` |

Las rutas coinciden con `com.circulo.auth0.constants.Auth0Constants`.

## Compilación y despliegue

- Requisitos: JDK 8, acceso a repositorios Liferay (`repository.liferay.com`).
- Compilar: desde esta carpeta, integrar el módulo en un **Liferay Gradle Workspace 7.3** y ejecutar el deploy habitual (`blade deploy` / `gradlew deploy`), o copiar el JAR generado al directorio `deploy` del portal.
- Ajustar la versión del BOM (`release.portal.bom`) en `build.gradle` si el fix pack del servidor difiere de `7.3.10`.

## Integraciones Liferay pendientes de validación

Las clases `PortalLoginServiceImpl` y `UserProvisioningServiceImpl` deben encapsular llamadas a APIs del kernel (`UserLocalService`, `SessionErrors`, autenticación, etc.). **No se asume** un único camino correcto sin contrastar políticas de seguridad y versión exacta del portal; quedan marcados con TODO y comentarios en código.

## Siguientes fases de implementación

1. PKCE: generar `code_verifier` / `code_challenge`, persistencia temporal (sesión HTTP u otro store acordado).
2. Login: redirección a `/authorize` con parámetros Auth0 y state/nonce.
3. Callback: intercambio de código por tokens (`Auth0TokenClient`), validación de `id_token` (`IdTokenValidator`).
4. Usuario y sesión: `UserProvisioningService` + `PortalLoginService` + `SessionTokenStore`.
5. Logout: sesión portal + logout federado Auth0 según configuración.
6. Proxy: `ApigeeProxyService` con cliente HTTP, inyección de bearer desde sesión, timeouts y manejo de errores.

## Licencia / uso interno

Uso previsto: proyecto Circulo Atención (privado). Ajustar metadatos legales según política de la organización.
