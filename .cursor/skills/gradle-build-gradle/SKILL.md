---
name: gradle-build-gradle
description: Trabaja con builds Gradle basados en build.gradle para módulos OSGi/Liferay (configuración, dependencias, tasks, repositorios, wrapper). Úsalo cuando el usuario mencione Gradle, build.gradle, Gradle wrapper, dependencies, configurations, o fallos de build.
---

# Gradle (build build.gradle)

## Objetivo
Aplicar un patrón consistente de **build Gradle** para módulos (típicamente OSGi en Liferay), manteniendo reproducibilidad con wrapper y separando claramente compile/runtime/test.

## Flujo recomendado cuando te pidan “arregla el build”
1. Identifica si el repo es mono-proyecto o multi-proyecto (`settings.gradle` / `settings.gradle.kts`).
2. Ubica el `build.gradle` relevante (root vs módulo).
3. Revisa:
   - `repositories` (y si hay repos privados)
   - `dependencies` (scope correcto)
   - `java` / compatibilidad (si es Java 8, alinear a 1.8)
   - tasks de empaquetado (jar/bnd/liferay)
4. Ejecuta (si procede) `gradlew clean test jar` o el task del módulo.

## Patrones útiles (preferidos en este contexto)
### Wrapper y plugins Liferay
- Preferir usar **`gradlew`** (no el Gradle instalado globalmente).
- Si hay fallos de versión: alinear `gradle-wrapper.properties` con el plugin/tooling requerido por Liferay.
  - **Importante:** El plugin `com.liferay.gradle.plugins:13.0.16` (usado en Liferay 7.3) **no es compatible** con Gradle 7+ ni 8+ debido a tareas antiguas como `BuildCSSTask` y configuración `compile`. Usa **Gradle 6.9.x** en el wrapper.

### Dependencias: scopes típicos en Liferay
- `compileOnly`: APIs provistas por el runtime (p.ej. Liferay, OSGi, JAX-RS).
- `compileInclude`: librerías de terceros que deben ir dentro del bundle (Bnd las empaquetará). **Nota:** en Gradle 6, dependencias con variantes complejas (como Guava 32+ transitiva) pueden requerir `resolutionStrategy { force ... }`.
- `testImplementation`: sólo tests.

Ejemplo:

```gradle
dependencies {
    compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "7.3.10.u32"

    compileInclude group: "com.auth0", name: "java-jwt", version: "3.19.4"
    compileInclude group: "com.auth0", name: "jwks-rsa", version: "0.22.1"
}
```

### Repositorios
- Si faltan artifacts, añade el repo correcto (Maven Central, repos corporativos, etc.) en el lugar apropiado (root para todos, o módulo si es específico).

## Diagnóstico rápido de errores comunes
- **“Could not find …”**: falta `repositories` o credenciales, o versión inexistente.
- **Conflictos de dependencias**: usar `dependencies`/`dependencyInsight` (si se ejecuta) para ver el árbol.
- **ClassNotFound en runtime OSGi**: dependencia debería ser `implementation` (embebida) o exportada/importada vía OSGi; revisa `bnd.bnd`.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Gradle (build build.gradle)”.
- Mensajes: `build.gradle`, `GradleException`, `Could not resolve`, `Task ... failed`.
