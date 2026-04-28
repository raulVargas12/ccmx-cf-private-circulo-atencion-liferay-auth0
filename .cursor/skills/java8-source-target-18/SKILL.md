---
name: java8-source-target-18
description: Configura y valida compatibilidad Java 8 (source/target 1.8) en módulos Gradle para Liferay DXP. Úsalo cuando el usuario mencione Java 8, 1.8, sourceCompatibility/targetCompatibility, bytecode level, o errores como "Unsupported major.minor version".
---

# Java 8 (source/target 1.8)

## Objetivo
Mantener **compilación y bytecode en Java 8** (1.8) en proyectos Gradle (típico en módulos OSGi para Liferay DXP 7.3).

## Checklist rápido (hazlo en este orden)
- Verifica el JDK real usado por Gradle (no solo el `JAVA_HOME`).
- Fuerza `sourceCompatibility`/`targetCompatibility` a `1.8` (o `options.release = 8` si aplica).
- Evita APIs de Java > 8 en el código (compila, pero falla en runtime si el portal es Java 8).
- Valida el bytecode generado (clase major version 52).

## Gradle: configuración recomendada
Si existe `build.gradle`, aplica **un default** para todos los subproyectos Java:

```gradle
allprojects {
    plugins.withId("java") {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        tasks.withType(JavaCompile).configureEach {
            options.encoding = "UTF-8"
        }
    }
}
```

Si el proyecto usa toolchains, prioriza Java 8:

```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}
```

## Validaciones y troubleshooting
- **Gradle usa otro JDK**: revisa `gradle.properties` y configuración del wrapper; confirma versión con `./gradlew -version`.
- **Bytecode incorrecto**: compila con JDK 11+ pero sin `target=1.8`; fuerza compatibilidad y recompila.
- **Errores de runtime** (NoSuchMethodError / ClassNotFound) tras compilar: suele ser API de Java > 8 o dependencia incompatible con Java 8.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Java 8 (source/target 1.8)”.
- Hay errores tipo: `UnsupportedClassVersionError`, “major.minor version”, o “has been compiled by a more recent version”.
