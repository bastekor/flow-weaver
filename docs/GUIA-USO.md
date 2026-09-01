# Guía de uso de flow-weaver

Esta guía explica **cómo consumir** la librería **flow-weaver** desde un proyecto Spring Boot: desde la integración inicial hasta el uso de las anotaciones y la sintaxis de expresiones.

La configuración por *properties* se describe por separado en [`CONFIGURACION.md`](./CONFIGURACION.md). La personalización de la salida se trata en [`PERSONALIZACION.md`](./PERSONALIZACION.md).

---

## Índice

- [1. Integración (dependencia)](#1-integración-dependencia)
- [2. Conceptos y arquitectura](#2-conceptos-y-arquitectura)
  - [2.1 BusinessLog vs AuditTrail](#21-businesslog-vs-audittrail)
  - [2.2 Identificadores generados](#22-identificadores-generados)
  - [2.3 Procesamiento asíncrono](#23-procesamiento-asíncrono)
- [3. Anotación `@BusinessLog`](#3-anotación-businesslog)
  - [3.1 Atributos](#31-atributos)
  - [3.2 Ejemplo completo](#32-ejemplo-completo)
- [4. Anotación `@AuditTrail`](#4-anotación-audittrail)
  - [4.1 Atributos](#41-atributos)
  - [4.2 Vincular con un `@BusinessLog`](#42-vincular-con-un-businesslog)
  - [4.3 AuditTrail huérfano (padrastro automático)](#43-audittrail-huérfano-padrastro-automático)
- [5. Anotación `@DataParam`](#5-anotación-dataparam)
- [6. Modos: STATIC, DYNAMIC y MERGED](#6-modos-static-dynamic-y-merged)
- [7. Sintaxis de expresiones](#7-sintaxis-de-expresiones)
  - [7.1 Alcances disponibles](#71-alcances-disponibles)
  - [7.2 Segmentos de ruta](#72-segmentos-de-ruta)
  - [7.3 Resolución de errores y fallback](#73-resolución-de-errores-y-fallback)
- [8. Contexto y propagación](#8-contexto-y-propagación)

---

## 1. Integración (dependencia)

La librería es un JAR de Spring Boot **auto-configurable**. Para usarla basta con agregar la dependencia al `pom.xml`:

```xml
<dependency>
    <groupId>mx.bastekor</groupId>
    <artifactId>flow-weaver</artifactId>
    <version>0.1.1-BETA</version>
</dependency>
```

Al arrancar, Spring Boot detecta el descriptor `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` y carga automáticamente `FlowWeaverConfig`, el cual:

- Habilita el proxy de aspectos (`@EnableAspectJAutoProxy`).
- Escanea el paquete `mx.bastekor.flowweaver` (`@ComponentScan`), registrando el aspecto, los servicios, los mappers y las configuraciones.
- Registra un bean `FlowWeaverResultHandler` por defecto (`LoggingFlowWeaverResultHandler`) si no defines el tuyo.

> **No necesitas escribir ninguna clase de configuración** en tu aplicación. Solo agrega la dependencia y anota tus métodos.

Matices de comportamiento que conviene conocer:

- El procesamiento del log se ejecuta **de forma asíncrona** en el executor `flowWeaverExecutor` (hilos `flow-weaver-*`). Tu método interceptado no se bloquea emitiendo el log.
- El `@EnableAspectJAutoProxy` y la propagación asíncrona de contexto ya vienen habilitados por la librería; no requieren configuración extra.

---

## 2. Conceptos y arquitectura

### 2.1 BusinessLog vs AuditTrail

| Concepto | `@BusinessLog` | `@AuditTrail` |
|---|---|---|
| Propósito | Bitácora **funcional** de negocio (qué se hizo). | **Auditoría** técnica/funcional (cómo se hizo). |
| Datos de entrada | Solo `dataOut` (salida). | `dataIn`, `dataOut` y `dataInOut`. |
| Vínculo con el padre | Es el flujo **padre**. | Puede referenciar a un `@BusinessLog` mediante `parentCode`. |
| Prefijo por defecto | `BL#` | `AT#` |

### 2.2 Identificadores generados

Si no defines (o no se resuelve) un código explícito, la librería genera uno con un prefijo (ver `FlowWeaverConstants`):

| Pieza | Prefijo |
|---|---|
| Group (flujo grupal) | `GC#XXXX-XXXX` |
| BusinessLog | `BL#XXXX-XXXX` |
| AuditTrail | `AT#XXXX-XXXX` |

> Para usar la configuración dinámica (`DYNAMIC`/`MERGED`) con `business-logs`/`audit-trails`, define siempre un `code` fijo; un código aleatorio `BL#...`/`AT#...` hace impracticable el mapeo por clave.

### 2.3 Procesamiento asíncrono

`FlowWeaverAspectService` procesa cada log con `@Async("flowWeaverExecutor")`. El executor (`AsyncConfig`) aplica un `TaskDecorator` que propaga el contexto de `FlowWeaverContext` al hilo secundario, de modo que los logs emitidos dentro de la misma ejecución conserven la relación de contexto (ver [§8](#8-contexto-y-propagación)).

---

## 3. Anotación `@BusinessLog`

`@Target(METHOD)`, `@Retention(RUNTIME)`. Se aplica sobre métodos para registrar la trazabilidad funcional del flujo.

### 3.1 Atributos

| Atributo | Tipo | Default | Descripción |
|---|---|---|---|
| `group` | `String` | `""` | Código grupal del flujo funcional. Si no resuelve, se infiere `GC#XXXX-XXXX`. |
| `code` | `String` | `""` | Código del flujo. Puede ser expresión (configuración externa) o texto directo. Si no resuelve, se infiere `BL#XXXX-XXXX`. |
| `description` | `String` | `""` | Expresión de la descripción del flujo. |
| `defaultDescription` | `String` | `""` | Fallback de `description` (texto plano). |
| `value` | `String` | `""` | Expresión del resultado en caso de éxito (contra `response`). |
| `defaultValue` | `String` | `""` | Fallback de `value` (texto plano). |
| `exception` | `String` | `""` | Expresión del valor en caso de excepción (contra `exception`). |
| `defaultException` | `String` | `""` | Fallback de `exception`. |
| `mode` | `Mode` | `STATIC` | `STATIC`, `DYNAMIC` o `MERGED` (ver [§6](#6-modos-static-dynamic-y-merged)). |
| `dataOut` | `DataParam[]` | `{}` | Datos de salida personalizados (ver [§5](#5-anotación-dataparam)). |

**Reglas de evaluación** (aplican a `description`/`value`/`exception` y a los `@DataParam`):

- Una propiedad se considera **expresión** si su valor no está vacío.
- Se intenta evaluar contra el contexto del método (p. ej. `args`, `response`, `exception`).
- Si la evaluación falla, se usa el `defaultX` correspondiente.
- Si ambos están vacíos, la propiedad se ignora.

### 3.2 Ejemplo completo

```java
@BusinessLog(
        group = "PAYMENTS",
        code = "EXECUTE-PAYMENT",
        description = "arg[1]",
        defaultDescription = "Ejecución de pago bancario",
        defaultValue = "Pago procesado exitosamente",
        dataOut = {
                @DataParam(key = "clientId", value = "request.clientId", defaultValue = "Desconocido"),
                @DataParam(key = "amount", value = "request.amount", defaultValue = "0"),
                @DataParam(key = "currency", value = "request.currency", defaultValue = "MXN")
        }
)
public PaymentResponse pay(PaymentRequest request, String message) {
    // ...
}
```

---

## 4. Anotación `@AuditTrail`

`@Target(METHOD)`, `@Retention(RUNTIME)`. Registra una pista de auditoría para un método.

### 4.1 Atributos

Además de los campos comunes (`group`, `code`, `description`/`defaultDescription`, `value`/`defaultValue`, `exception`/`defaultException`, `mode`), introduce:

| Atributo | Tipo | Default | Descripción |
|---|---|---|---|
| `parentCode` | `String` | `""` | Código del `@BusinessLog` padre, para trazabilidad cruzada. |
| `dataIn` | `DataParam[]` | `{}` | Datos a extraer de la entrada (`args`). |
| `dataOut` | `DataParam[]` | `{}` | Datos a extraer de la salida (`response`). |
| `dataInOut` | `DataParam[]` | `{}` | Datos comunes a entrada y salida. |

### 4.2 Vincular con un `@BusinessLog`

```java
@AuditTrail(
        parentCode = "EXECUTE-PAYMENT",
        code = "AUDIT-OPERATION",
        defaultDescription = "Registrando operación en auditoría",
        defaultValue = "Operación auditada correctamente",
        dataIn = {
                @DataParam(key = "operation", value = "args[0]", defaultValue = "N/A")
        },
        dataOut = {
                @DataParam(key = "result", value = "response", defaultValue = "N/A")
        }
)
public String audit(String operation, String detail) {
    return "AUDIT-" + operation + "-" + detail;
}
```

### 4.3 AuditTrail huérfano (padrastro automático)

Si el `@AuditTrail` no define un `parentCode` resolubles, la librería genera un `BusinessLogContainer` "padrastro" por defecto usando la convención:

```
{application-name}_class_{className}#methodName
```

Ejemplo: aplicación `payments-service`, método `miMetodo()` dentro de `AService` → `payments-service_class_AService#miMetodo`.

Esto garantiza que todo evento tenga al menos un identificador de flujo padre. Este contenedor huérfano se limpia al final de la ejecución. (Detalle interno del `FlowWeaverContext`; no requiere acción del consumidor.)

---

## 5. Anotación `@DataParam`

`@Target(ANNOTATION_TYPE)`, `@Retention(RUNTIME)`. Define un parámetro individual de entrada/salida. Se usa únicamente **dentro** de otra anotación (`@BusinessLog.dataOut`, `@AuditTrail.dataIn/dataOut/dataInOut`).

| Atributo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `key` | `String` | **Sí** | Nombre del campo en la salida final. |
| `value` | `String` | No | Expresión a evaluar (p. ej. `response.data.id`, `args[0].nombre`). |
| `defaultValue` | `String` | No | Fallback si `value` no resuelve. |

```java
@BusinessLog(
        dataOut = {
                @DataParam(key = "userId",   value = "response.user.id",   defaultValue = "0"),
                @DataParam(key = "username", value = "args[0].username",   defaultValue = "desconocido")
        }
)
```

> Si `key` está vacío o no se definen `value`/`defaultValue`, el campo se omite.

---

## 6. Modos: STATIC, DYNAMIC y MERGED

El `mode` (definido en la anotación o en la configuración) determina de dónde provienen los valores:

| Modo | Comportamiento |
|---|---|
| `STATIC` (default) | Valores tomados **directamente de la anotación**. Se ignora la configuración dinámica. |
| `DYNAMIC` | Valores tomados **exclusivamente de la configuración** (`flow-weaver.business-logs[code]` / `flow-weaver.audit-trails[code]`). Si no hay entrada, no hay datos. |
| `MERGED` | Combina ambos. **La configuración tiene prioridad**: por campo se toma el valor no-vacío de la configuración y, si falta, el de la anotación. Los `@DataParam` se mezclan por `key`. |

> Se recomienda `STATIC` para usos simples e inline; `MERGED` cuando quieras sobreescribir ciertos valores desde `application.yml` sin tocar código; `DYNAMIC` cuando todo el detalle viva en la configuración.

Ejemplo de `MERGED`: el `@BusinessLog` define `defaultValue = "Pago procesado"`, y la configuración define `default-value: "Pago acreditado en X"`. En `MERGED`, ganará el de la configuración.

---

## 7. Sintaxis de expresiones

Las expresiones (`description`, `value`, `exception`, `@DataParam.value`) son **rutas de acceso** que navegan sobre un árbol JSON (la firma del método serializada). El resolvedor (`ExpressionResolver`) es 100% nativo sobre JSON: no ejecuta SpEL ni código arbitrario.

### 7.1 Alcances disponibles

La librería expone varios *alcances raíz* a los que apunta la expresión. Los más usados:

| Alcance | Corresponde a | Ejemplo |
|---|---|---|
| `request` | Argumento (request) del método | `request.clientId` |
| `args` | Arreglo de todos los argumentos | `args[0]`, `args[1].nombre` |
| `response` / `result` | Objeto de retorno del método | `response.message` |
| `exception` | Excepción lanzada | `exception.message` |
| `arg` | (alias no indexado, vía root scope) | `arg[1]` |

> En el ejemplo real de `PaymentService` se usa `arg[1]` y `request.clientId`. El alcance raíz se resuelve mediante `ExpressionResolver.resolve(jsonSnapshot, rootScope, expression)`, donde `rootScope` apunta al nodo raíz correspondiente al contexto.

### 7.2 Segmentos de ruta

Los segmentos se separan con punto (`.`). Un segmento puede incluir índice entre corchetes.

| Notación | Descripción |
|---|---|
| `campo` | Lookup de campo en un objeto / clave de mapa. |
| `campo.subcampo` | Navegación encadenada. |
| `[n]` | Índice **positivo** de arreglo (0-based). |
| `[-n]` | Índice **negativo** de arreglo (desde el final; `[-1]` es el último). |
| `[texto]` | Clave literal de mapa/propiedad (sin comillas). |
| `["texto"]` | Clave literal de mapa con comillas dobles. |
| `['texto']` | Clave literal de mapa con comillas simples. |
| `n` | Número desnudo como índice de arreglo. |
| `campo[n]` | Se expande a dos pasos: campo + índice. |

Ejemplos:

```text
response.status.code          → response.getStatus().getCode()
response.message              → response.getMessage()
args[0].nombre                → primer argumento, campo nombre
request.items[-1].price       → último elemento del arreglo items
metadata["pedido.id"]         → clave literal que contiene un punto
```

### 7.3 Resolución de errores y fallback

- Si una expresión no se resuelve, se usa el `defaultX` de la propiedad.
- Si `show-errors` está activo (`flow-weaver.frame.show-errors`), el `LoggingFlowWeaverResultHandler` agrega una sección **Flow Weaver Errors** bajo el frame, mostrando por cada clave fallida el mensaje y las sugerencias (campos disponibles en el nodo, recordatorios de sintaxis de arreglos, etc.).

---

## 8. Contexto y propagación

`FlowWeaverContext` mantiene un `ThreadContainer` por ejecución (almacenado en el hilo actual), que agrupa el `BusinessLogContainer` actual y sus `AuditTrailContainer`s hijo.

- Al interceptar un `@BusinessLog`, se crea/asigna el `BusinessLogContainer` (`assignBusinessLogContainer`) para el flujo.
- Los `@AuditTrail` vinculados (por `parentCode`) se asocian a ese contenedor padre; los huérfanos crean un "padrastro" automático (ver [§4.3](#43-audittrail-huérfano-padrastro-automático)).
- Al terminar, el contexto se limpia (`clearBusinessLogContainer`).

Como el procesamiento del log es **asíncrono**, `AsyncConfig.FlowWeaverTaskDecorator` propaga el `ThreadContainer` padre al hilo secundario **solo si** el hilo hijo no tiene uno propio, y lo limpia al final. Esto preserva la coherencia de contexto entre la ejecución síncrona del método y el log asíncrono.
