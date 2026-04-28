---
name: liferay-jsonfactoryutil
description: Parseo y construcción de JSON con JSONFactoryUtil en Liferay: JSONObject/JSONArray, lectura segura de campos, manejo de nulls y errores al consumir respuestas (Auth0/JWKS). Úsalo cuando el usuario mencione JSONFactoryUtil, JSONObject, JSONArray, parseo de JSON, JWKS JSON, o respuestas de Auth0.
---

# JSON de Liferay (JSONFactoryUtil) para parseo de respuestas y JWKS

## Objetivo
Parsear JSON en Liferay de forma consistente usando `JSONFactoryUtil`, especialmente para respuestas de Auth0 (token) y JWKS.

## Reglas prácticas
- Usa `JSONFactoryUtil.createJSONObject(String)` para parsear.
- Lee campos con métodos “opt”/safe cuando existan (o valida existencia antes de acceder).
- Trata explícitamente `null`, tipos inesperados y campos ausentes (APIs cambian).

## JWKS: estructura típica a considerar
- `keys`: array
- cada key: `kid`, `kty`, `use`, `alg`, `n`, `e`, `x5c` (según proveedor)
- Selección por `kid` (no por posición)

## Errores comunes
- Parseo asumiendo tipo fijo (string vs number).
- No manejar arrays vacíos o `kid` ausente.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “JSON de Liferay (JSONFactoryUtil)”.
- Se menciona parseo de respuestas HTTP, JWKS JSON, o errores por campos faltantes.
