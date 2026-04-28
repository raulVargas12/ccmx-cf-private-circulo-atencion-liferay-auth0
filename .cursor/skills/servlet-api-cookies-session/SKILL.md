---
name: servlet-api-cookies-session
description: Maneja cookies y sesión con Servlet API (HttpServletRequest/Response/Session) en Java/Liferay: flags de cookie, SameSite, secure, HttpOnly, CSRF/state, y persistencia de valores de auth. Úsalo cuando el usuario mencione HttpServletRequest, HttpServletResponse, HttpSession, cookies, sesión, o problemas de login/state perdido.
---

# Servlet API (HttpServletRequest/Response/Session) para cookies y sesión

## Objetivo
Persistir datos de autenticación de forma segura usando **cookies** y/o **HttpSession** (p.ej. PKCE verifier, state/nonce, tokens por sesión).

## Cookies: defaults seguros
- `HttpOnly` para evitar acceso desde JS cuando no se necesita.
- `Secure` si hay HTTPS (recomendado en producción).
- `SameSite` acorde al flujo (Lax/None):
  - Para redirects cross-site (Auth0) puede requerir `SameSite=None; Secure`.
- `Path` y `Max-Age` explícitos.

## Sesión (HttpSession)
- Guardar datos efímeros del login (PKCE/state/nonce).
- Limpiar al finalizar el callback (evitar replays y leaks).
- Evitar almacenar tokens de largo plazo si no es necesario; preferir expiración y refresh controlado.

## Fallas típicas
- **state/verifier perdido**: cookie bloqueada por SameSite/Secure, o session affinity en cluster.
- **loops de login**: cookie/path/domain mal configurado o sesión invalidándose.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Servlet API (HttpServletRequest/Response/Session) para cookies y sesión”.
- Se menciona cookies, sesión, callback OIDC, state, SameSite, o problemas intermitentes en cluster.
