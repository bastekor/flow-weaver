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
- [5. Contrato de estatus para implementadores](#5-contrato-de-estatus-para-implementadores)
- [6. Ejemplo completo](#6-ejemplo-completo)
- [7. Buenas prácticas](#7-buenas-prácticas)

---

## 1. Punto de extensión: `FlowWeaverResultHandler`

La interfaz pública de personalización es:

```java
public interface FlowWeaverResultHandler {
    void handle(String methodDuration, String mappedDuration, RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException;
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
| `processStatus` | `StatusEnum` | Estatus de procesamiento de flow-weaver (por defecto `INTERNAL_SUCCESS`; en reintentos conserva el estatus de la falla). |

---

## 4. Cómo registrar tu propio handler

La librería usa `@ConditionalOnMissingBean(FlowWeaverResultHandler.class)`: si tu aplicación define un bean de tipo `FlowWeaverResultHandler`, Spring **usará el tuyo** en lugar de `LoggingFlowWeaverResultHandler`.

En tu proyecto de consumo, crea una clase `@Component` que implemente la interfaz:

```java
@Component
public class MyResultHandler implements FlowWeaverResultHandler {
    @Override
    public void handle(String methodDuration, String mappedDuration, RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException {
        // tu lógica
    }
}
```

> Por el `@ComponentScan(basePackages = "mx.bastekor.flowweaver")` de la librería, basta con que tu bean sea un `@Component`/`@Service`/`@Bean` en cualquier paquete escaneado por **tu** aplicación.

---

## 5. Contrato de estatus para implementadores

`StatusEnum` agrupa los estatus por **origen**. Antes de implementar tu handler, entiende quién usa cada grupo:

| Grupo | Quién lo usa | Uso |
|---|---|---|
| `SOURCE_*` (`SOURCE_SUCCESS` / `SOURCE_FAILURE`) | El **aspecto** (`FlowWeaverAspect`) | Resultado del método de negocio interceptado. No es responsabilidad del handler. |
| `INTERNAL_*` (`INTERNAL_SUCCESS` / `INTERNAL_FAILURE` / `INTERNAL_ERROR`) | La **librería** y su `LoggingFlowWeaverResultHandler` | Procesamiento interno: éxito, falla recuperable e invariante de implementación. No es responsabilidad del handler. |
| `EXTERNAL_*` (`EXTERNAL_SUCCESS` / `EXTERNAL_FAILURE`) | **Tú, el implementador del handler** | Frontera de entrega/persistencia hacia afuera de la librería. **Obligatorios para tu handler.** |

> **Regla:** quien implemente `FlowWeaverResultHandler` **debe** utilizar `EXTERNAL_*`. Los `INTERNAL_*` son responsabilidad interna de la librería y no deben emitirse desde el handler de un consumidor; si tu handler no logra entregar/persistir el resultado, lanza `FlowWeaverException(..., EXTERNAL_FAILURE)`.

`EXTERNAL_SUCCESS` aplica cuando quieres reportar una entrega correcta de forma explícita; `EXTERNAL_FAILURE`, cuando la entrega/persistencia falla. El framework captura ambos y los registra sin romper el flujo del negocio.

---

## 6. Ejemplo completo

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
    public void handle(String methodDuration, String mappedDuration, RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException {
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

## 7. Buenas prácticas

- **No bloquees el hilo del negocio**: `handle` se invoca en el executor asíncrono, pero aún así mantenlo rápido; el fallo aquí no revierte la transacción del método.
- **Usa siempre `EXTERNAL_*` en tu handler** (ver [§5](#5-contrato-de-estatus-para-implementadores)): lanza `FlowWeaverException(..., EXTERNAL_FAILURE)` cuando no puedas entregar/persistir el resultado.
- **No emitas `INTERNAL_*` desde tu handler**: son de la librería; si los usas, interfieres con su mecanismo de recuperación de reintentos.
- **Conserva `requestDTO` para el detalle completo** y `fields` para el formato plano/ligero.
- Si solo quieres cambiar **el formato** (no el destino), usa `flow-weaver.frame.*` en la configuración en lugar de escribir un handler (ver [`CONFIGURACION.md`](./CONFIGURACION.md)).
