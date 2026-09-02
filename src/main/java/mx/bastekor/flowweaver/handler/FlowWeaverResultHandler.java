package mx.bastekor.flowweaver.handler;

import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;

import java.util.Map;

public interface FlowWeaverResultHandler {

    /**
     *
     * @param methodDuration Duración en "texto" que tomó el método anotado en ejecutarse
     * @param mappedDuration Duración en "texto" que tomó el mapeo de datos en flow-weaver
     * @param requestDTO     Objeto mapeado con los valores resueltos de las anotaciones
     * @param fields         Mapa de campos planos en posición "key=value" sin ingreso en objetos.
     * @throws FlowWeaverException posible excepción a manejar.
     */
    void handle(String methodDuration, String mappedDuration, RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException;
}