package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataParamMapperTest {

    @Mock
    private DataParam dataParam;

    @Test
    void nullInput_returnsNull() {
        assertNull(DataParamMapper.createDataParamsDTO(null));
    }

    @Test
    void emptyArray_returnsEmptyArray() {
        DataParamDTO[] result = DataParamMapper.createDataParamsDTO(new DataParam[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mapsFieldsCorrectly() {
        when(dataParam.key()).thenReturn("k1");
        when(dataParam.value()).thenReturn("v1");
        when(dataParam.defaultValue()).thenReturn("d1");

        DataParamDTO[] result = DataParamMapper.createDataParamsDTO(new DataParam[]{dataParam});

        assertEquals(1, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("v1", result[0].getValue());
        assertEquals("d1", result[0].getDefaultValue());
    }

    @Test
    void skipsNullElement() {
        when(dataParam.key()).thenReturn("k");
        when(dataParam.value()).thenReturn("v");
        when(dataParam.defaultValue()).thenReturn("d");

        DataParamDTO[] result = DataParamMapper.createDataParamsDTO(new DataParam[]{dataParam, null});

        assertEquals(2, result.length);
        assertNotNull(result[0]);
        assertEquals("k", result[0].getKey());
        assertNull(result[1]);
    }

    @Test
    void mapsMultipleElements() {
        DataParam[] arr = new DataParam[2];
        when(dataParam.key()).thenReturn("k1");
        when(dataParam.value()).thenReturn("v1");
        when(dataParam.defaultValue()).thenReturn("d1");
        arr[0] = dataParam;

        DataParam dataParam2 = org.mockito.Mockito.mock(DataParam.class);
        when(dataParam2.key()).thenReturn("k2");
        when(dataParam2.value()).thenReturn("v2");
        when(dataParam2.defaultValue()).thenReturn("d2");
        arr[1] = dataParam2;

        DataParamDTO[] result = DataParamMapper.createDataParamsDTO(arr);

        assertEquals(2, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("k2", result[1].getKey());
    }
}
