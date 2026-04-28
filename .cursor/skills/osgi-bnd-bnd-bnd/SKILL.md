---
name: osgi-bnd-bnd-bnd
description: Define y depura bundles OSGi usando BND (archivo bnd.bnd): headers, Import-Package/Export-Package, Private-Package, Bundle-SymbolicName y versionado. Úsalo cuando el usuario mencione bnd.bnd, BND, OSGi manifest, Import-Package, Export-Package, o errores de resolución/wiring.
---

# OSGi (bundle OSGi con BND: bnd.bnd)

## Objetivo
Construir bundles OSGi **predecibles** con `bnd.bnd`, minimizando problemas de wiring (imports/exports) en Liferay.

## Checklist esencial de `bnd.bnd`
- `Bundle-SymbolicName`: único y estable.
- `Bundle-Version`: versionado semántico (alineado al release del módulo).
- `Private-Package`: tu código (lo que NO exportas).
- `Export-Package`: sólo si de verdad necesitas exponer API.
- `Import-Package`: dejar que BND calcule por defecto y ajustar sólo casos especiales.

## Patrón recomendado (mínimo)

```properties
Bundle-Name: ${project.name}
Bundle-SymbolicName: com.example.my.module
Bundle-Version: 1.0.0

Private-Package: com.example.my.module.*
```

## Ajustes comunes en Liferay
- Evita exportar paquetes sin necesidad; en DXP muchos consumos son vía servicios OSGi, no por `Export-Package`.
- Si embebes librerías (por `implementation`), revisa si duplican paquetes que el portal ya exporta.

## Diagnóstico rápido de errores
- **Bundle no arranca / unresolved requirements**: falta `Import-Package` o rangos de versión incompatibles.
- **Uses constraint violation**: conflicto por duplicar libs ya provistas por el portal; preferir una sola fuente de exportación.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “OSGi (bundle OSGi con BND: bnd.bnd)”.
- Se ven errores: `Unable to resolve`, `uses constraint violation`, `Import-Package`, `Export-Package`.
