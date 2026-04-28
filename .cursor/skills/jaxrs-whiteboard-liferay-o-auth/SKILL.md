---
name: jaxrs-whiteboard-liferay-o-auth
description: Implementa endpoints REST con JAX-RS Whiteboard en Liferay bajo /o/auth/*: propiedades osgi.jaxrs.*, application/base, recursos, JSON y seguridad. Úsalo cuando el usuario mencione JAX-RS, Whiteboard, /o/, endpoints REST, osgi.jaxrs.application.base, o problemas 404 en rutas.
---

# JAX-RS / JAX-RS Whiteboard (endpoints REST bajo /o/auth/*)

## Objetivo
Exponer endpoints REST en Liferay usando **JAX-RS Whiteboard** con base `/o/auth`.

## Patrón recomendado (Whiteboard)
- Publica una `Application` con:
  - `osgi.jaxrs.application.base=/auth`
  - `osgi.jaxrs.name=...`
- Publica resources JAX-RS como servicios OSGi.

## Checklist de wiring
- Ruta completa esperada: `GET /o/auth/...`
- Si retorna 404:
  - `osgi.jaxrs.application.base` no coincide
  - el recurso no está registrado como servicio
  - el bundle no está activo o hay DS unsatisfied

## Respuestas JSON
- En Liferay, si se pide, preferir `JSONFactoryUtil` para parse/compose cuando se integra con APIs del portal.
- Si usas JAX-RS estándar, mantener DTOs simples y serialización consistente.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “JAX-RS / JAX-RS Whiteboard (endpoints REST bajo /o/auth/*)”.
- Se menciona `/o/auth`, “Whiteboard”, 404/No route, o propiedades `osgi.jaxrs.*`.
