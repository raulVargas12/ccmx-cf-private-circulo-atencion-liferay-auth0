---
name: jwt-rs256-auth0-java-jwt
description: Firma/valida JWT RS256 usando la librería Auth0 java-jwt en Java/Liferay: verificación de claims (iss/aud/exp/nbf), clock skew, manejo de errores y separación de parse vs verify. Úsalo cuando el usuario mencione JWT, RS256, java-jwt, id_token/access_token, claims, o validación de token.
---

# JWT (RS256) con librería Auth0 `java-jwt`

## Objetivo
Validar JWT RS256 correctamente: **no basta con decodificar**, hay que **verificar firma** y claims.

## Reglas de validación
- Verifica firma RS256 con la llave pública correcta (derivada de JWKS).
- Verifica al menos:
  - `iss` (issuer esperado)
  - `aud` (audience esperado)
  - `exp` (no expirado)
  - `nbf`/`iat` (si aplica)
- Permite un clock skew pequeño y controlado (segundos, no minutos).

## Buenas prácticas
- Separa:
  - **decode/parse**: leer header/payload (sin confianza)
  - **verify**: verificar firma + claims (confianza)
- Nunca aceptes algoritmos inesperados: fuerza RS256.
- Propaga errores con causa clara: expirado vs firma inválida vs issuer/audience.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “JWT (RS256) con librería Auth0 java-jwt”.
- Se ve: “token invalid”, “signature verification failed”, claims mismatch, expirations.
