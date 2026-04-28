---
name: liferay-dxp-7310-u32-api
description: Aplica convenciones de desarrollo para Liferay DXP 7.3.10 U32 usando com.liferay.portal:release.dxp.api:7.3.10.u32 (scopes, compatibilidad, y uso de APIs). Úsalo cuando el usuario mencione Liferay DXP 7.3, 7.3.10.u32, release.dxp.api, Portal, OSGi modules, o Service Builder/DXP APIs.
---

# Liferay DXP 7.3.10 U32 (release.dxp.api)

## Objetivo
Desarrollar módulos compatibles con **Liferay DXP 7.3.10 U32**, usando la API agregada `com.liferay.portal:release.dxp.api:7.3.10.u32` como **API provista por el portal**.

## Regla de oro (dependencia)
En Gradle, esta dependencia va típicamente como `compileOnly`:

```gradle
dependencies {
    compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "7.3.10.u32"
}
```

## Checklist de compatibilidad
- Evita usar clases internas no-API (paquetes `com.liferay.portal.kernel.*` sí suelen ser API; `com.liferay.portal.impl.*` no).
- Mantén Java alineado al runtime del portal (comúnmente Java 8 en 7.3).
- Si agregas librerías externas, decide si deben ir:
  - dentro del bundle (normalmente `implementation`)
  - como módulos OSGi separados
  - o provistas por el portal (evitar duplicarlas si el portal ya las trae)

## Diagnóstico típico en Liferay/OSGi
- **NoClassDefFoundError / ClassNotFoundException** en runtime:
  - La dependencia era `compileOnly` pero debía ir embebida (`implementation`), o
  - Falta `Import-Package`/`Require-Capability` correcto (revisar `bnd.bnd`).
- **Uses constraint violation**:
  - Conflicto de versiones/exportaciones OSGi (evitar empaquetar librerías que el portal exporta, o aislar con shading/fragment cuando aplique).

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Liferay DXP 7.3.10 U32 (com.liferay.portal:release.dxp.api:7.3.10.u32)”.
- Se mencionan módulos OSGi, portal upgrades, hotfix/updates, o errores de resolución OSGi en DXP 7.3.
