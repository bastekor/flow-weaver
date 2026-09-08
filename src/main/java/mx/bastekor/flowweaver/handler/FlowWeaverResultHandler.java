package mx.bastekor.flowweaver.handler;

import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;
import mx.bastekor.flowweaver.dto.FlowWeaverResponse;

import java.util.Map;

public interface FlowWeaverResultHandler {

    /**
     * @param rs Objeto con los valores resueltos finales.
     * rs.methodDuration Duración en "texto" que tomó el método anotado en ejecutarse
     * rs.mappedDuration Duración en "texto" que tomó el mapeo de datos en flow-weaver
     * rs.requestDTO     Objeto mapeado con los valores resueltos de las anotaciones
     * rs.fields         Mapa de campos planos en posición "key=value" sin ingreso en objetos.
     * @throws FlowWeaverException posible excepción a manejar.
     */
    void handle(FlowWeaverResponse rs) throws FlowWeaverException;
}