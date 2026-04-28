---
name: liferay-multivmpool-portalcache
description: Usa caché de clúster de Liferay (MultiVMPool/PortalCache) para almacenar tokens por sesión y por usuario, con claves estables, TTL, invalidación y concurrencia. Úsalo cuando el usuario mencione MultiVMPool, PortalCache, cluster cache, tokens por sesión/usuario, o problemas de sticky sessions.
---

# Caché de clúster de Liferay: MultiVMPool / PortalCache (tokens por sesión y por usuario)

## Objetivo
Cachear tokens/datos de auth de forma **consistente en cluster**, evitando depender de sticky sessions.

## Diseño de claves (regla práctica)
- Define namespace fijo (p.ej. `auth0:`).
- Claves separadas:
  - Por sesión: `auth0:session:{sessionId}:...`
  - Por usuario: `auth0:user:{userId}:...`
- Evita PII en la clave; usa IDs internos.

## TTL e invalidación
- TTL alineado a `exp` del token (nunca más largo).
- Invalida al logout y al detectar token inválido/rotado.
- Maneja concurrencia: “cache stampede” (single-flight / locks ligeros) si muchos threads piden lo mismo.

## Fallas típicas
- **Tokens “desaparecen”**: TTL muy corto o claves inconsistentes.
- **Inconsistencias en cluster**: usando cache local en vez de MultiVM, o dependencia en sesión local.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Caché de clúster de Liferay: MultiVMPool / PortalCache”.
- Se habla de cluster, sesiones no pegadas, o caching de tokens.
