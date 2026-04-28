---
name: jwks-rsa-auth0-cache
description: Resuelve llaves RSA desde JWKS usando jwks-rsa (Auth0) con caché propia: selección por kid, rotación de llaves, TTL, negative caching y fallback ante errores. Úsalo cuando el usuario mencione JWKS, jwks-rsa, kid, rotating keys, cache, o verificación RS256 fallando por llaves.
---

# JWKS (RSA keys) con `jwks-rsa` + caché propia

## Objetivo
Obtener llaves públicas RSA desde JWKS de forma **eficiente** y **resiliente** a rotación (`kid`).

## Reglas del resolver
- Selecciona la key por `kid` del header JWT.
- Si `kid` no existe en cache:
  - consulta JWKS
  - actualiza cache
  - reintenta resolución
- Soporta rotación:
  - TTL razonable
  - refresh bajo demanda cuando hay `kid` desconocido

## Caché propia (patrón)
- Cache key: `issuer + kid` (o `jwksUri + kid`).
- Guardar:
  - key material (public key)
  - `fetchedAt` / `expiresAt`
- Considera **negative caching** corto para `kid` inexistente para evitar thundering herd.

## Errores comunes
- **“No keys found for kid”**: JWKS desactualizado en cache o issuer equivocado.
- **Timeout al JWKS**: degradar con cache existente si no expiró; si expiró, fallar seguro.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “JWKS (RSA keys) con librería Auth0 jwks-rsa + caché propia”.
- Se menciona `kid`, rotación de llaves, o fallas intermitentes de verificación RS256.
