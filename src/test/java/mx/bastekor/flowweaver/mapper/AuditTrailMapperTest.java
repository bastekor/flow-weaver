package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static mx.bastekor.flowweaver.factory.TestFactory.anAuditTrailContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditTrailMapperTest {

    @Mock
    private AuditTrail auditTrail;

    @Mock
    private DataParam dataParamIn;

    @Mock
    private DataParam dataParamOut;

    @Mock
    private DataParam dataParamInOut;

    @Test
    void nullContainer_returnsNull() {
        assertNull(AuditTrailMapper.createAuditTrailDTO(null));
    }

    @Test
    void nullAuditTrail_returnsNull() {
        AuditTrailContainer container = new AuditTrailContainer();
        assertNull(AuditTrailMapper.createAuditTrailDTO(container));
    }

    @Test
    void mapsAllFieldsCorrectly() {
        when(auditTrail.description()).thenReturn("desc");
        when(auditTrail.defaultDescription()).thenReturn("defaultDesc");
        when(auditTrail.value()).thenReturn("val");
        when(auditTrail.defaultValue()).thenReturn("defaultVal");
        when(auditTrail.exception()).thenReturn("ex");
        when(auditTrail.defaultException()).thenReturn("defaultEx");
        when(auditTrail.mode()).thenReturn(Mode.MERGED);

        AuditTrailContainer container = anAuditTrailContainer("test-group", "BL-001", "AT-001", auditTrail);

        AuditTrailDTO result = AuditTrailMapper.createAuditTrailDTO(container);

        assertEquals("test-group", result.getGroup());
        assertEquals("BL-001", result.getFlowCode());
        assertEquals("AT-001", result.getCode());
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
        when(auditTrail.description()).thenReturn("  padded desc  ");
        when(auditTrail.defaultDescription()).thenReturn("  padded default  ");
        when(auditTrail.value()).thenReturn("  padded value  ");
        when(auditTrail.defaultValue()).thenReturn("  padded default value  ");
        when(auditTrail.exception()).thenReturn("  padded ex  ");
        when(auditTrail.defaultException()).thenReturn("  padded default ex  ");
        when(auditTrail.mode()).thenReturn(Mode.STATIC);

        AuditTrailContainer container = anAuditTrailContainer(auditTrail);

        AuditTrailDTO result = AuditTrailMapper.createAuditTrailDTO(container);

        assertEquals("padded desc", result.getDescription());
        assertEquals("padded default", result.getDefaultDescription());
        assertEquals("padded value", result.getValue());
        assertEquals("padded default value", result.getDefaultValue());
        assertEquals("padded ex", result.getException());
        assertEquals("padded default ex", result.getDefaultException());
    }

    @Test
    void mapsDataIn() {
        when(dataParamIn.key()).thenReturn("k-in");
        when(dataParamIn.value()).thenReturn("v-in");
        when(dataParamIn.defaultValue()).thenReturn("d-in");
        when(auditTrail.dataIn()).thenReturn(new DataParam[]{dataParamIn});
        when(auditTrail.dataOut()).thenReturn(new DataParam[0]);
        when(auditTrail.dataInOut()).thenReturn(new DataParam[0]);
        when(auditTrail.description()).thenReturn("d");
        when(auditTrail.defaultDescription()).thenReturn("dd");
        when(auditTrail.value()).thenReturn("v");
        when(auditTrail.defaultValue()).thenReturn("dv");
        when(auditTrail.exception()).thenReturn("e");
        when(auditTrail.defaultException()).thenReturn("de");
        when(auditTrail.mode()).thenReturn(Mode.DYNAMIC);

        AuditTrailContainer container = anAuditTrailContainer(auditTrail);

        AuditTrailDTO result = AuditTrailMapper.createAuditTrailDTO(container);

        assertNotNull(result.getDataIn());
        assertEquals(1, result.getDataIn().length);
        assertEquals("k-in", result.getDataIn()[0].getKey());
        assertEquals("v-in", result.getDataIn()[0].getValue());
        assertEquals("d-in", result.getDataIn()[0].getDefaultValue());
    }

    @Test
    void mapsDataOut() {
        when(dataParamOut.key()).thenReturn("k-out");
        when(dataParamOut.value()).thenReturn("v-out");
        when(dataParamOut.defaultValue()).thenReturn("d-out");
        when(auditTrail.dataIn()).thenReturn(new DataParam[0]);
        when(auditTrail.dataOut()).thenReturn(new DataParam[]{dataParamOut});
        when(auditTrail.dataInOut()).thenReturn(new DataParam[0]);
        when(auditTrail.description()).thenReturn("d");
        when(auditTrail.defaultDescription()).thenReturn("dd");
        when(auditTrail.value()).thenReturn("v");
        when(auditTrail.defaultValue()).thenReturn("dv");
        when(auditTrail.exception()).thenReturn("e");
        when(auditTrail.defaultException()).thenReturn("de");
        when(auditTrail.mode()).thenReturn(Mode.DYNAMIC);

        AuditTrailContainer container = anAuditTrailContainer(auditTrail);

        AuditTrailDTO result = AuditTrailMapper.createAuditTrailDTO(container);

        assertNotNull(result.getDataOut());
        assertEquals(1, result.getDataOut().length);
        assertEquals("k-out", result.getDataOut()[0].getKey());
        assertEquals("v-out", result.getDataOut()[0].getValue());
        assertEquals("d-out", result.getDataOut()[0].getDefaultValue());
    }

    @Test
    void mapsDataInOut() {
        when(dataParamInOut.key()).thenReturn("k-inout");
        when(dataParamInOut.value()).thenReturn("v-inout");
        when(dataParamInOut.defaultValue()).thenReturn("d-inout");
        when(auditTrail.dataIn()).thenReturn(new DataParam[0]);
        when(auditTrail.dataOut()).thenReturn(new DataParam[0]);
        when(auditTrail.dataInOut()).thenReturn(new DataParam[]{dataParamInOut});
        when(auditTrail.description()).thenReturn("d");
        when(auditTrail.defaultDescription()).thenReturn("dd");
        when(auditTrail.value()).thenReturn("v");
        when(auditTrail.defaultValue()).thenReturn("dv");
        when(auditTrail.exception()).thenReturn("e");
        when(auditTrail.defaultException()).thenReturn("de");
        when(auditTrail.mode()).thenReturn(Mode.DYNAMIC);

        AuditTrailContainer container = anAuditTrailContainer(auditTrail);

        AuditTrailDTO result = AuditTrailMapper.createAuditTrailDTO(container);

        assertNotNull(result.getDataInOut());
        assertEquals(1, result.getDataInOut().length);
        assertEquals("k-inout", result.getDataInOut()[0].getKey());
        assertEquals("v-inout", result.getDataInOut()[0].getValue());
        assertEquals("d-inout", result.getDataInOut()[0].getDefaultValue());
    }
}
