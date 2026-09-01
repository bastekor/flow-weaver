# flow-weaver

Librería de **Spring Boot auto-configurable** para trazabilidad funcional y auditoría. Permite registrar bitácoras de negocio (`@BusinessLog`) y pistas de auditoría (`@AuditTrail`) de forma declarativa mediante anotaciones y AOP, con soporte de expresiones dinámicas sobre la firma de los métodos.

- **Java 17** · **Spring Boot 3.5.x** · **Maven**

---

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/GUIA-USO.md`](./docs/GUIA-USO.md) | **Guía de uso**: integración, anotaciones (`@BusinessLog`, `@AuditTrail`, `@DataParam`), modos, sintaxis de expresiones y contexto. |
| [`docs/CONFIGURACION.md`](./docs/CONFIGURACION.md) | Todas las propiedades configurables (`flow-weaver.*`): business-logs, audit-trails, frame y max-depth. Incluye [`application.example.yml`](./docs/application.example.yml). |
| [`docs/PERSONALIZACION.md`](./docs/PERSONALIZACION.md) | Punto de extensión `FlowWeaverResultHandler`: cómo personalizar la salida del log. |

---

## Uso rápido

Agrega la dependencia y anota un método:

```xml
<dependency>
    <groupId>mx.bastekor</groupId>
    <artifactId>flow-weaver</artifactId>
    <version>0.1.1-BETA</version>
</dependency>
```

```java
@BusinessLog(
        group = "PAYMENTS",
        code = "EXECUTE-PAYMENT",
        defaultValue = "Pago procesado exitosamente",
        dataOut = {
                @DataParam(key = "clientId", value = "request.clientId", defaultValue = "Desconocido")
        }
)
public PaymentResponse pay(PaymentRequest request, String message) {
    // ... lógica de negocio
}
```

Al ejecutarse, se emite (por defecto en consola, de forma asíncrona) el log de resultado con el código, descripción, estado, resultado y los datos extraídos. Consulta la [guía de uso](./docs/GUIA-USO.md) para los detalles completos.

---

## Características

- **Auto-configuración**: solo con agregar el JAR al `pom.xml` la librería se activa (aspecto, executor asíncrono y handler por defecto).
- **Anotaciones** `@BusinessLog`, `@AuditTrail` y `@DataParam`.
- **Modos** `STATIC`, `DYNAMIC` y `MERGED` para combinar anotación y configuración.
- **Expresiones** sobre la firma JSON del método (`ExpressionResolver`) con fallback por defecto y sugerencias de resolución.
- **Contexto** por ejecución con propagación a hilos asíncronos (`AsyncConfig`).
- **Extensible**: implementa `FlowWeaverResultHandler` para personalizar la salida ([`PERSONALIZACION.md`](./docs/PERSONALIZACION.md)).
