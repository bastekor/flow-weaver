# Configuración de flow-weaver

Este documento describe todas las propiedades configurables de **flow-weaver**. La librería se configura mediante propiedades de Spring Boot (`application.yml` o `application.properties`). El prefijo raíz es `flow-weaver`.

Un ejemplo listo para copiar se encuentra en [`docs/application.example.yml`](./application.example.yml).

---

## Índice

- [1. Prefijos de configuración](#1-prefijos-de-configuración)
- [2. Propiedades de framework](#2-propiedades-de-framework)
  - [2.1 `flow-weaver.max-depth`](#21-flow-weavermax-depth)
- [3. Business Logs dinámicos (`flow-weaver.business-logs`)](#3-business-logs-dinámicos-flow-weaverbusiness-logs)
  - [3.1 Campos de cada BusinessLog](#31-campos-de-cada-businesslog)
  - [3.2 Modos: STATIC, DYNAMIC y MERGED](#32-modos-static-dynamic-y-merged)
- [4. Audit Trails dinámicos (`flow-weaver.audit-trails`)](#4-audit-trails-dinámicos-flow-weaveraudit-trails)
- [5. Marco de salida (`flow-weaver.frame`)](#5-marco-de-salida-flow-weaverframe)
  - [5.1 Propiedades del frame](#51-propiedades-del-frame)
  - [5.2 Salida DUAL_LINE](#52-salida-dual_line)
  - [5.3 Salida SINGLE_LINE](#53-salida-single_line)
  - [5.4 Ejemplos de exclusión y errores](#54-ejemplos-de-exclusión-y-errores)
- [6. Equivalencia con `application.properties`](#6-equivalencia-con-applicationproperties)

---

## 1. Prefijos de configuración

| Prefijo | Clase de configuración | Descripción |
|---|---|---|
| `flow-weaver` | `FlowWeaverRootConfig` | Propiedades raíz: mapas de business-logs/audit-trails y `max-depth`. |
| `flow-weaver.frame` | `FrameConfig` | Configuración del formato de salida del log. |

---

## 2. Propiedades de framework

### 2.1 `flow-weaver.max-depth`

Profundidad máxima de anidamiento al serializar la firma de entrada y salida del método anotado (`@BusinessLog` / `@AuditTrail`). Permite acotar el tamaño del JSON generado en objetos anidados.

| Tipo | Default |
|---|---|
| `int` | `3` |

```yaml
flow-weaver:
  max-depth: 5
```

```properties
flow-weaver.max-depth=5
```

---

## 3. Business Logs dinámicos (`flow-weaver.business-logs`)

Mapa de configuración de los `@BusinessLog`. **La clave es el `code` del `@BusinessLog`.**

Se emplea cuando la anotación tiene `mode = DYNAMIC` o `mode = MERGED` (ver [§3.2](#32-modos-static-dynamic-y-merged)). En `STATIC` este mapa se ignora porque los valores provienen directamente de la anotación.

> **Nota importante:** Si el `@BusinessLog` no define un `code` explícito, la librería genera uno con prefijo `BL#...` (aleatorio), lo cual hace impráctico usar la configuración dinámica. Para DYNAMIC/MERGED define siempre un `code` fijo.

```yaml
flow-weaver:
  business-logs:
    EXECUTE-PAYMENT:
      group: PAYMENTS
      code: EXECUTE-PAYMENT
      description: arg[1]
      mode: MERGED
      # ... resto de campos
```

### 3.1 Campos de cada BusinessLog

Cada entrada del mapa corresponde a un `BusinessLogDTO` con los siguientes campos:

| Campo | Tipo | Descripción |
|---|---|---|
| `correlationId` | `String` | Identificador de correlación (se ignora al hacer merge en `UtilMapper`). |
| `group` | `String` | Grupo/Categoría del flujo de negocio. |
| `code` | `String` | Código del flujo de negocio. Coincide con la clave del mapa. |
| `description` | `String` | Expresión a resolver para obtener la descripción. Soporta rutas JSON (p. ej. `arg[1]`, `request.clientId`). |
| `defaultDescription` | `String` | Descripción por defecto si la expresión no resuelve. |
| `value` | `String` | Expresión a resolver para el resultado principal (p. ej. `response.message`). |
| `defaultValue` | `String` | Valor por defecto del resultado si no resuelve. |
| `exception` | `String` | Expresión a resolver para el mensaje de excepción (p. ej. `exception.message`). |
| `defaultException` | `String` | Valor por defecto del mensaje de excepción. |
| `mode` | `Mode` | `STATIC`, `DYNAMIC` o `MERGED`. |
| `dataIn` | `DataParamDTO[]` | Parámetros de entrada a extraer. Estructura `{ key, value, defaultValue }`. |
| `dataOut` | `DataParamDTO[]` | Parámetros de salida a extraer. Estructura `{ key, value, defaultValue }`. |
| `dataInOut` | `DataParamDTO[]` | Parámetros comunes de entrada/salida. Estructura `{ key, value, defaultValue }`. |

**Cada `DataParamDTO` usa tres campos:**

| Campo | Descripción |
|---|---|
| `key` | Clave con la que se expondrá el valor (se usa como nombre del campo en la salida). |
| `value` | Expresión a resolver (ruta JSON sobre la firma/response). |
| `defaultValue` | Valor por defecto si la expresión no resuelve. |

```yaml
flow-weaver:
  business-logs:
    EXECUTE-PAYMENT:
      group: PAYMENTS
      code: EXECUTE-PAYMENT
      description: arg[1]
      default-description: Ejecución de pago bancario
      value: response.message
      default-value: Pago procesado exitosamente
      exception: exception.message
      default-exception: Error en el pago
      mode: MERGED
      data-out:
        - { key: clientId, value: request.clientId, default-value: "Valor desconocido" }
        - { key: amount, value: request.amount, default-value: "0" }
```

### 3.2 Modos: STATIC, DYNAMIC y MERGED

El `mode` puede declararse tanto en la anotación como en esta configuración. Determina el origen de los valores:

| Modo | Comportamiento |
|---|---|
| `STATIC` | Los valores se toman **directamente de la anotación**. El mapa de configuración se ignora. |
| `DYNAMIC` | Los valores se toman **exclusivamente de la configuración** (`business-logs[code]`). Si no existe entrada, no hay datos. |
| `MERGED` | Combina ambos. **La configuración tiene prioridad** sobre la anotación: por cada campo se toma el valor no-vacío de la configuración y, si falta, el de la anotación. Si no hay entrada de configuración, se usan los valores de la anotación. |

En `MERGED`, campos del `DataParamDTO[]` se combinan por su `key`: los `key` presentes en la configuración reemplazan/mezclan a los de la anotación, y los que solo existan en la anotación se conservan.

---

## 4. Audit Trails dinámicos (`flow-weaver.audit-trails`)

Mapa de configuración de los `@AuditTrail`. **La clave es el `code` del `@AuditTrail`** (no el `parentCode`/`flowCode`).

Cada entrada es un `AuditTrailDTO`, que **hereda todos los campos de `BusinessLogDTO`**, más un campo adicional:

| Campo | Tipo | Descripción |
|---|---|---|
| `flowCode` | `String` | Código del flujo de negocio padre (equivale al `parentCode` del `@AuditTrail`). |

```yaml
flow-weaver:
  audit-trails:
    DEBIT-ACCOUNT:
      group: PAYMENTS
      flow-code: EXECUTE-PAYMENT
      code: DEBIT-ACCOUNT
      default-description: Aplicando cargo a cuenta de origen
      default-value: Cargo aplicado correctamente
      mode: MERGED
      data-out:
        - { key: account, value: request.sourceAccount, default-value: "XXXX-XXXX-XXXX-XXXX" }
```

Aplica la misma preferencia de clave que `business-logs`: define un `code` fijo en la anotación si usarás DYNAMIC/MERGED.

---

## 5. Marco de salida (`flow-weaver.frame`)

Configura cómo se imprime el log de resultado que genera la librería (`FrameFormatter`).

### 5.1 Propiedades del frame

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `output-mode` | `OutputMode` | `DUAL_LINE` | `DUAL_LINE` o `SINGLE_LINE`. |
| `entry-separator` | `String` | `\|` | Separador entre campos. |
| `pair-separator` | `String` | `=` | Separador clave/valor (solo en `SINGLE_LINE`). |
| `skip-nulls` | `boolean` | `false` | Omite campos con valor `null`. |
| `skip-blanks` | `boolean` | `false` | Omite campos vacíos o solo espacios. |
| `excluded-keys` | `List<String>` | `[]` | Claves a excluir de la salida. |
| `show-errors` | `boolean` | `false` | Muestra la sección de errores de resolución (fallback). |

```yaml
flow-weaver:
  frame:
    output-mode: DUAL_LINE
    entry-separator: "|"
    pair-separator: "="
    skip-nulls: false
    skip-blanks: false
    excluded-keys:
      - hostName
      - ipAddress
    show-errors: true
```

### 5.2 Salida DUAL_LINE

Las claves en una línea y los valores en la siguiente, separados por `entry-separator`:

```
request_id|group|code|description|status|result|mode
abc-123|PAYMENTS|EXECUTE-PAYMENT|Pago|SOURCE_SUCCESS|OK|STATIC
```

### 5.3 Salida SINGLE_LINE

Pares `clave=valor` separados por `entry-separator`:

```
request_id=abc-123|group=PAYMENTS|code=EXECUTE-PAYMENT|status=SOURCE_SUCCESS
```

> En `SINGLE_LINE`, `entry-separator` y `pair-separator` **deben ser distintos**; si son iguales, `FrameFormatter` lanza `IllegalArgumentException`.

### 5.4 Ejemplos de exclusión y errores

- **`excluded-keys`**: si agregas, por ejemplo, `hostName` e `ipAddress`, esos campos no se muestran en el frame.
- **`show-errors`**: al activarlo, si alguna expresión no se resolvió (cayó en fallback), se agrega una sección de errores bajo el frame mostrando la clave, el mensaje y las sugerencias de cada resolución fallida.

---

## 6. Equivalencia con `application.properties`

Todas las propiedades se pueden escribir también en formato `.properties`:

```properties
flow-weaver.max-depth=5

flow-weaver.frame.output-mode=DUAL_LINE
flow-weaver.frame.entry-separator=|
flow-weaver.frame.pair-separator==
flow-weaver.frame.skip-nulls=false
flow-weaver.frame.skip-blanks=false
flow-weaver.frame.show-errors=true
flow-weaver.frame.excluded-keys[0]=hostName
flow-weaver.frame.excluded-keys[1]=ipAddress

# BusinessLogs dinámicos
flow-weaver.business-logs.EXECUTE-PAYMENT.group=PAYMENTS
flow-weaver.business-logs.EXECUTE-PAYMENT.code=EXECUTE-PAYMENT
flow-weaver.business-logs.EXECUTE-PAYMENT.description=arg[1]
flow-weaver.business-logs.EXECUTE-PAYMENT.default-description=Ejecución de pago
flow-weaver.business-logs.EXECUTE-PAYMENT.mode=MERGED
flow-weaver.business-logs.EXECUTE-PAYMENT.data-out[0].key=clientId
flow-weaver.business-logs.EXECUTE-PAYMENT.data-out[0].value=request.clientId
flow-weaver.business-logs.EXECUTE-PAYMENT.data-out[0].default-value=Desconocido

# AuditTrails dinámicos
flow-weaver.audit-trails.DEBIT-ACCOUNT.group=PAYMENTS
flow-weaver.audit-trails.DEBIT-ACCOUNT.code=DEBIT-ACCOUNT
flow-weaver.audit-trails.DEBIT-ACCOUNT.flow-code=EXECUTE-PAYMENT
flow-weaver.audit-trails.DEBIT-ACCOUNT.mode=MERGED
```
