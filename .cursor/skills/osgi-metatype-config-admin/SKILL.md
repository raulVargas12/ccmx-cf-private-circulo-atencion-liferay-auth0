---
name: osgi-metatype-config-admin
description: Define y consume configuración OSGi Metatype/Config Admin en Liferay (System Settings y archivos .config): @Meta.OCD/@Meta.AD, configurationPid, y lectura en @Activate. Úsalo cuando el usuario mencione System Settings, Config Admin, .config, metatype, PID, o configuración OSGi.
---

# OSGi Metatype / Config Admin (System Settings / .config)

## Objetivo
Modelar configuración OSGi de forma que sea **editable en System Settings** y **aplicable por Config Admin** (incluyendo despliegue por `.config`).

## Patrón recomendado (interfaz @Meta)
- Define un `@Meta.OCD` con `id` estable (PID).
- Define campos con `@Meta.AD` (nombres/descr. útiles).
- En el componente DS, usa `@Activate` para mapear `Map<String, Object>` a config tipada.

## Checklist de PIDs
- PID debe ser **estable** y coincidir con lo que publicas en System Settings o `.config`.
- Si usas `configurationPid`, asegúrate de no duplicar/variar accidentalmente.

## Archivo `.config` (deployment)
- Usar cuando se quiere configuración por entorno (dev/qa/prod) sin tocar UI.
- Mantener valores sensibles fuera del repo (si aplica); preferir secret management del entorno.

## Diagnóstico rápido
- **No aparece en System Settings**: faltan metatype headers o el módulo no generó metatype correctamente.
- **No aplica valores**: PID equivocado o el componente no se reactiva al cambiar config.
- **Valores default siempre**: el componente no está leyendo/mapeando la config en `@Activate`.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “OSGi Metatype / Config Admin”.
- Se menciona: “System Settings”, `.config`, “PID”, “configuration”.
