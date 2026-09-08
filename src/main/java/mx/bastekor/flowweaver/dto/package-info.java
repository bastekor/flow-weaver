/**
 * Portadores de datos anémicos (sin comportamiento) que se transfieren o
 * mapean entre capas y hacia el consumidor del framework.
 * <p>
 * Coincide con el resultado de recolección/modificación/mapeo: son
 * transitorios. Un *DTO no debe contener lógica de negocio.
 * <p>
 * Lo que NO va aquí: estado mutable con comportamiento → {@code context};
 * helpers estáticos sin estado → {@code util}.
 */
package mx.bastekor.flowweaver.dto;