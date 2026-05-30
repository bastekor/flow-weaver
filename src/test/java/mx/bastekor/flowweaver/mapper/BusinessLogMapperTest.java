package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static mx.bastekor.flowweaver.factory.TestFactory.aBusinessLogContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessLogMapperTest {

    @Mock
    private BusinessLog businessLog;

    @Mock
    private DataParam dataParam;

    @Test
    void nullContainer_returnsNull() {
        assertNull(BusinessLogMapper.createBusinessLogDTO(null));
    }

    @Test
    void nullBusinessLog_returnsNull() {
        assertNull(BusinessLogMapper.createBusinessLogDTO(new BusinessLogContainer("corr", "group", "code")));
    }

    @Test
    void mapsAllFieldsCorrectly() {
        when(businessLog.description()).thenReturn("desc");
        when(businessLog.defaultDescription()).thenReturn("defaultDesc");
        when(businessLog.value()).thenReturn("val");
        when(businessLog.defaultValue()).thenReturn("defaultVal");
        when(businessLog.exception()).thenReturn("ex");
        when(businessLog.defaultException()).thenReturn("defaultEx");
        when(businessLog.mode()).thenReturn(Mode.MERGED);

        BusinessLogContainer container = aBusinessLogContainer("corr-1", "test-group", "BL-001", businessLog);

        BusinessLogDTO result = BusinessLogMapper.createBusinessLogDTO(container);

        assertEquals("test-group", result.getGroup());
        assertEquals("BL-001", result.getCode());
        assertEquals("desc", result.getDescription());
        assertEquals("defaultDesc", result.getDefaultDescription());
        assertEquals("val", result.getValue());
        assertEquals("defaultVal", result.getDefaultValue());
        assertEquals("ex", result.getException());
        assertEquals("defaultEx", result.getDefaultException());
        assertEquals(Mode.MERGED, result.getMode());
    }

    @Test
    void trimsStringFields() {
        when(businessLog.description()).thenReturn("  padded desc  ");
        when(businessLog.defaultDescription()).thenReturn("  padded default  ");
        when(businessLog.value()).thenReturn("  padded value  ");
        when(businessLog.defaultValue()).thenReturn("  padded default value  ");
        when(businessLog.exception()).thenReturn("  padded ex  ");
        when(businessLog.defaultException()).thenReturn("  padded default ex  ");
        when(businessLog.mode()).thenReturn(Mode.STATIC);

        BusinessLogContainer container = aBusinessLogContainer(businessLog);

        BusinessLogDTO result = BusinessLogMapper.createBusinessLogDTO(container);

        assertEquals("padded desc", result.getDescription());
        assertEquals("padded default", result.getDefaultDescription());
        assertEquals("padded value", result.getValue());
        assertEquals("padded default value", result.getDefaultValue());
        assertEquals("padded ex", result.getException());
        assertEquals("padded default ex", result.getDefaultException());
    }

    @Test
    void mapsDataOut() {
        when(dataParam.key()).thenReturn("k1");
        when(dataParam.value()).thenReturn("v1");
        when(dataParam.defaultValue()).thenReturn("d1");
        when(businessLog.dataOut()).thenReturn(new DataParam[]{dataParam});
        when(businessLog.description()).thenReturn("desc");
        when(businessLog.defaultDescription()).thenReturn("dd");
        when(businessLog.value()).thenReturn("v");
        when(businessLog.defaultValue()).thenReturn("dv");
        when(businessLog.exception()).thenReturn("e");
        when(businessLog.defaultException()).thenReturn("de");
        when(businessLog.mode()).thenReturn(Mode.DYNAMIC);

        BusinessLogContainer container = aBusinessLogContainer(businessLog);

        BusinessLogDTO result = BusinessLogMapper.createBusinessLogDTO(container);

        assertNotNull(result.getDataOut());
        assertEquals(1, result.getDataOut().length);
        assertEquals("k1", result.getDataOut()[0].getKey());
        assertEquals("v1", result.getDataOut()[0].getValue());
        assertEquals("d1", result.getDataOut()[0].getDefaultValue());
    }

    @Test
    void emptyDataOut_returnsEmptyArray() {
        when(businessLog.dataOut()).thenReturn(new DataParam[0]);
        when(businessLog.description()).thenReturn("d");
        when(businessLog.defaultDescription()).thenReturn("dd");
        when(businessLog.value()).thenReturn("v");
        when(businessLog.defaultValue()).thenReturn("dv");
        when(businessLog.exception()).thenReturn("e");
        when(businessLog.defaultException()).thenReturn("de");
        when(businessLog.mode()).thenReturn(Mode.STATIC);

        BusinessLogContainer container = aBusinessLogContainer(businessLog);

        BusinessLogDTO result = BusinessLogMapper.createBusinessLogDTO(container);

        assertNotNull(result.getDataOut());
        assertEquals(0, result.getDataOut().length);
    }
}
