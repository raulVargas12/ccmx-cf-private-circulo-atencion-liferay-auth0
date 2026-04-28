---
name: osgi-declarative-services-ds
description: Implementa y depura OSGi Declarative Services (DS) con org.osgi.service.component.annotations: @Component, @Activate/@Deactivate, @Reference, configuraciones y lifecycle en Liferay. Úsalo cuando el usuario mencione DS, @Component, @Reference, SCR/Declarative Services, activación, o componentes que no arrancan.
---

# Declarative Services (DS) (org.osgi.service.component.annotations)

## Objetivo
Crear componentes DS robustos para Liferay/OSGi: **lifecycle claro**, referencias estables y configuración correcta.

## Reglas prácticas
- Preferir `@Activate` para inicialización mínima y segura.
- En `@Reference`, define cardinalidad/política sólo si hace falta; si no, usa defaults.
- Evita lógica pesada en `@Activate` (puede bloquear activación del bundle).
- Usa `immediate = true` sólo cuando el componente debe activarse sin consumidores.

## Plantilla típica

```java
@Component(service = MyService.class)
public class MyServiceImpl implements MyService {

    @Activate
    protected void activate() {
        // init rápido y seguro
    }

    @Reference
    protected void setSomeDependency(SomeDependency dep) {
        _dep = dep;
    }

    private volatile SomeDependency _dep;
}
```

## Diagnóstico rápido (cuando “no levanta”)
- **Componente unsatisfied**:
  - Falta servicio referenciado o el filtro LDAP no matchea.
  - El bundle que provee la referencia no está activo/exportando.
- **Activate falla**:
  - Excepción en `@Activate` o config inválida.
- **Configuración no aplicada**:
  - `configurationPid` incorrecto, o PID distinto al que se está configurando en System Settings.

## Señales de que debes aplicar este skill
- El usuario pide explícitamente: “Declarative Services (DS)”.
- Se mencionan: `@Component`, `@Reference`, “unsatisfied”, “SCR”, “activate/deactivate”.
