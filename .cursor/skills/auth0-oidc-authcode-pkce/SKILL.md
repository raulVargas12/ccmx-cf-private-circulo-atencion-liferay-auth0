---
name: auth0-oidc-authcode-pkce
description: Implementa Auth0/OIDC OAuth2 Authorization Code + PKCE en backend Java/Liferay: redirect/login, code_verifier/challenge, intercambio de code por tokens, validación de state/nonce y manejo de errores. Úsalo cuando el usuario mencione Auth0, OIDC, OAuth2, authorization code, PKCE, code_verifier, callback, o login redirect.
---

# Auth0 / OIDC OAuth2 Authorization Code + PKCE

## Objetivo
Implementar el flujo **Authorization Code + PKCE** de forma segura para un cliente web, con backend en Java/Liferay.

## Reglas de seguridad (no negociables)
- Genera `code_verifier` fuerte (alta entropía) por intento de login.
- Deriva `code_challenge` (S256) del verifier.
- Usa `state` para CSRF y valida en el callback.
- Si aplica OIDC, usa `nonce` y valida en el ID Token.
- En el callback, **intercambia code** por tokens usando el `code_verifier`.

## Persistencia temporal
- Guarda `code_verifier` (y `state`/`nonce`) en **sesión** o en una cookie segura ligada a sesión.
- Expira/borra los valores tras completar el intercambio.

## Errores comunes
- **Invalid state**: state no coincide o se perdió la sesión/cookie.
- **Invalid_grant**: code ya usado, expirado, redirect_uri no coincide, o verifier incorrecto.
- **Mixed content/redirect mismatch**: `redirect_uri` debe ser idéntico al registrado.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Auth0 / OIDC OAuth2 Authorization Code + PKCE”.
- Se menciona callback, `code_verifier`, `code_challenge`, `state`, `nonce`, Auth0 authorize/token endpoints.
