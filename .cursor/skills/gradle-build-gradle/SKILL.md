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
### Wrapper
- Preferir usar **`gradlew`** (no el Gradle instalado globalmente).
- Si hay fallos de versión: alinear `gradle-wrapper.properties` con el plugin/tooling requerido por Liferay.

### Dependencias: scopes típicos
- `compileOnly`: APIs provistas por el runtime (p.ej. Liferay)
- `implementation`: librerías que deben ir dentro del bundle
- `testImplementation`: sólo tests

Ejemplo:

```gradle
dependencies {
    compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "7.3.10.u32"

    implementation group: "com.auth0", name: "java-jwt", version: "REPLACE_ME"
    implementation group: "com.auth0", name: "jwks-rsa", version: "REPLACE_ME"
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
