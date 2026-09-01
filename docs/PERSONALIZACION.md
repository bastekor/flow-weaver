# Personalización de la salida en flow-weaver

Este documento explica cómo **personalizar la emisión del log** de flow-weaver implementando tu propio `FlowWeaverResultHandler`. Útil cuando quieres enviar el resultado a un bus de eventos, una base de datos, un endpoint, o un formato propio, en lugar del log por consola por defecto.

---

## Índice

- [1. Punto de extensión: `FlowWeaverResultHandler`](#1-punto-de-extensión-flowweaverresulthandler)
- [2. El handler por defecto](#2-el-handler-por-defecto)
- [3. Datos que recibe tu handler](#3-datos-que-recibe-tu-handler)
  - [3.1 `requestDTO`](#31-requestdto)
  - [3.2 `fields`](#32-fields)
- [4. Cómo registrar tu propio handler](#4-cómo-registrar-tu-propio-handler)
- [5. Ejemplo completo](#5-ejemplo-completo)
- [6. Buenas prácticas](#6-buenas-prácticas)

---

## 1. Punto de extensión: `FlowWeaverResultHandler`

La interfaz pública de personalización es:

```java
public interface FlowWeaverResultHandler {
    void handle(RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException;
}
```

`FlowWeaverAspectService` construye el `RequestDTO` (vía `RequestDTOMapper`), extrae el frame (vía `FrameExtractor` con `max-depth`) e invoca a `handle(...)` al final de cada `@BusinessLog` / `@AuditTrail`. Lo que hagas dentro de `handle` es tu decisión: imprimir, persistir, publicar, etc.

---

## 2. El handler por defecto

Si no defines uno propio, la librería usa `LoggingFlowWeaverResultHandler` (registrado con `@ConditionalOnMissingBean` en `FlowWeaverConfig`). Imprime en consola:

```
##################################################
 - Flow Weaver Result(id|group|code)
<frame formateado según FrameConfig>
 - Flow Weaver Errors(id|group|code)   (solo si show-errors y hay fallos)
##################################################
```

Este handler se puede **sobreescribir/deshabilitar** con `flow-weaver.frame` (formato del frame) y con `flow-weaver.frame.show-errors` (sección de errores). Para cambiar el destino o el formato de raíz, define tu propio `FlowWeaverResultHandler`.

---

## 3. Datos que recibe tu handler

### 3.1 `requestDTO`

`RequestDTO` extiende `SimpleRequestDTO` y expone un mapa `resolutions`:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` (`request_id`) | `String` | Identificador único del log. |
| `group` | `String` | Grupo del flujo. |
| `code` | `String` | Código del flujo. |
| `description` | `String` | Descripción resuelta. |
| `status` | `String` | Estado/resultado de la operación. |
| `result` | `String` | Resultado principal. |
| `mode` | `String` | Modo aplicado (`STATIC`/`DYNAMIC`/`MERGED`). |
| `phase` | `Phase` | Fase del flujo. |
| `data` | `DataDTO` | Datos de entrada/salida (`in`, `out`, `in-out`). |
| `resolutions` | `Map<String, ResolutionResult>` | Resoluciones de cada campo, con su estado y error (ver abajo). |

**`ResolutionResult`** (en `mx.bastekor.flowweaver.resolver`) contiene: snapshot, alcance, expresión, sugerencia de ruta, valor resuelto, si falló (boolean), duración y ruta resuelta; y un `ResolutionError` (mensaje + sugerencias) en caso de fallo.

> `requestDTO` se serializa (vía su `toString()` / JSON) con `@JsonInclude(NON_NULL)`, por lo que los campos nulos no aparecen en la salida por defecto.

### 3.2 `fields`

`Map<String, Object>` con el frame ya aplanado por `FrameExtractor` (sigue `max-depth`, `excluded-keys`, etc.), más claves adicionales:

| Clave | Tipo | Descripción |
|---|---|---|
| (claves del frame) | `Object` | Campos del `RequestDTO` aplanados y sus valores (según `FrameConfig`). |
| `methodDuration` | `String` | Duración del método interceptado. |
| `mappedDuration` | `String` | Duración del mapeo de datos. |
| `flowWeaverException` | `FlowWeaverException` | Presente **solo** si falló la extracción del frame. |

---

## 4. Cómo registrar tu propio handler

La librería usa `@ConditionalOnMissingBean(FlowWeaverResultHandler.class)`: si tu aplicación define un bean de tipo `FlowWeaverResultHandler`, Spring **usará el tuyo** en lugar de `LoggingFlowWeaverResultHandler`.

En tu proyecto de consumo, crea una clase `@Component` que implemente la interfaz:

```java
@Component
public class MyResultHandler implements FlowWeaverResultHandler {
    @Override
    public void handle(RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException {
        // tu lógica
    }
}
```

> Por el `@ComponentScan(basePackages = "mx.bastekor.flowweaver")` de la librería, basta con que tu bean sea un `@Component`/`@Service`/`@Bean` en cualquier paquete escaneado por **tu** aplicación.

---

## 5. Ejemplo completo

Enviar el resultado a un logger estructurado y, además, a un repositorio (seudocódigo de integración):

```java
package com.miapp.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;
import mx.bastekor.flowweaver.handler.FlowWeaverResultHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonResultHandler implements FlowWeaverResultHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException {
        try {
            // 1. Emite el frame en una sola línea JSON.
            log.info(objectMapper.writeValueAsString(fields));

            // 2. Publica el requestDTO completo a tu sistema de auditoría.
            auditRepository.save(requestDTO);
        } catch (Exception e) {
            // Al lanzar FlowWeaverException, el framework registra el error interno.
            throw new FlowWeaverException("No se pudo procesar el resultado", StatusEnum.EXTERNAL_FAILURE, e);
        }
    }
}
```

> Nota: `StatusEnum` se importa desde `mx.bastekor.flowweaver.enums`. Lanzar `FlowWeaverException` permite al framework clasificar el error (`EXTERNAL_FAILURE`) sin romper el flujo del consumidor.

---

## 6. Buenas prácticas

- **No bloquees el hilo del negocio**: `handle` se invoca en el executor asíncrono, pero aún así mantenlo rápido; el fallo aquí no revierte la transacción del método.
- **Maneja tus errores**: lanza `FlowWeaverException` con el `StatusEnum` adecuado para que el framework registre el fallo (`EXTERNAL_FAILURE` para la salida y `INTERNAL_FAILURE` para el mapeo).
- **Conserva `requestDTO` para el detalle completo** y `fields` para el formato plano/ligero.
- Si solo quieres cambiar **el formato** (no el destino), usa `flow-weaver.frame.*` en la configuración en lugar de escribir un handler (ver [`CONFIGURACION.md`](./CONFIGURACION.md)).
