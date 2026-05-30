package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.enums.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilMapperTest {

    private final UtilMapper mapper = new UtilMapperImpl();

    // ============================================================
    //  mergeDataParamDTO — property-by-property merge
    // ============================================================

    @Test
    void mergeDataParamDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeDataParamDTO(null, null));
    }

    @Test
    void mergeDataParamDTO_priorityTakesPrecedence() {
        DataParamDTO priority = new DataParamDTO("k", "p-v", "p-d");
        DataParamDTO fallback = new DataParamDTO("k", "f-v", "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("k", result.getKey());
        assertEquals("p-v", result.getValue());
        assertEquals("p-d", result.getDefaultValue());
    }

    @Test
    void mergeDataParamDTO_priorityPartialFallbackFills() {
        DataParamDTO priority = new DataParamDTO("k", "p-v", null);
        DataParamDTO fallback = new DataParamDTO("k", null, "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("p-v", result.getValue());
        assertEquals("f-d", result.getDefaultValue());
    }

    @Test
    void mergeDataParamDTO_priorityBlankFallbackFills() {
        DataParamDTO priority = new DataParamDTO("k", "", "");
        DataParamDTO fallback = new DataParamDTO("k", "f-v", "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("f-v", result.getValue());
        assertEquals("f-d", result.getDefaultValue());
    }

    // ============================================================
    //  mergeDataParamDTOArrays — array merge by key
    // ============================================================

    @Test
    void mergeDataParamDTOArrays_bothNull_returnsEmptyArray() {
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(null, null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_priorityNull_returnsFilteredFallback() {
        DataParamDTO[] fallback = {new DataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(null, fallback);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackNull_returnsFilteredPriority() {
        DataParamDTO[] priority = {new DataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, null);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_priorityEmpty_returnsFilteredFallback() {
        DataParamDTO[] fallback = {new DataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(new DataParamDTO[0], fallback);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackEmpty_returnsFilteredPriority() {
        DataParamDTO[] priority = {new DataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_matchingKeys_mergesProperties() {
        DataParamDTO[] priority = {new DataParamDTO("k1", "p-v", "p-d")};
        DataParamDTO[] fallback = {new DataParamDTO("k1", "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(1, result.length);
        assertEquals("p-v", result[0].getValue());
        assertEquals("p-d", result[0].getDefaultValue());
    }

    @Test
    void mergeDataParamDTOArrays_matchingKeys_priorityBlankUsesFallback() {
        DataParamDTO[] priority = {new DataParamDTO("k1", "", null)};
        DataParamDTO[] fallback = {new DataParamDTO("k1", "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals("f-v", result[0].getValue());
        assertEquals("f-d", result[0].getDefaultValue());
    }

    @Test
    void mergeDataParamDTOArrays_priorityOrphansKept() {
        DataParamDTO[] priority = {new DataParamDTO("k1", "v1", "d1"), new DataParamDTO("k2", "v2", "d2")};
        DataParamDTO[] fallback = {new DataParamDTO("k3", "v3", "d3")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(3, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("k2", result[1].getKey());
        assertEquals("k3", result[2].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackOrphansAdded() {
        DataParamDTO[] priority = {new DataParamDTO("k1", "p-v", "p-d")};
        DataParamDTO[] fallback = {new DataParamDTO("k1", "f-v", "f-d"), new DataParamDTO("k2", "f-v2", "f-d2")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(2, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("p-v", result[0].getValue());
        assertEquals("k2", result[1].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_nullKeyDiscarded() {
        DataParamDTO[] priority = {new DataParamDTO(null, "v1", "d1"), new DataParamDTO("k", "v2", "d2")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_blankKeyDiscarded_returnsEmptyArray() {
        DataParamDTO[] priority = {new DataParamDTO(" ", "v1", "d1")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_emptyKeyDiscarded_returnsEmptyArray() {
        DataParamDTO[] priority = {new DataParamDTO("", "v1", "d1")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_nullKeyInFallbackIgnored() {
        DataParamDTO[] priority = {new DataParamDTO("k", "p-v", "p-d")};
        DataParamDTO[] fallback = {new DataParamDTO(null, "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
        assertEquals("p-v", result[0].getValue());
    }

    @Test
    void mergeDataParamDTOArrays_mixedScenario() {
        DataParamDTO[] priority = {
                new DataParamDTO("k1", "p-v1", "p-d1"),
                new DataParamDTO("k2", "p-v2", "p-d2"),
                new DataParamDTO(null, "p-v3", null),
                new DataParamDTO("", "p-v4", null)
        };
        DataParamDTO[] fallback = {
                new DataParamDTO("k1", "f-v1", "f-d1"),
                new DataParamDTO("k3", "f-v3", "f-d3"),
                new DataParamDTO("  ", "f-v4", null)
        };
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);

        assertEquals(3, result.length);
        // k1 merged, priority wins
        assertEquals("k1", result[0].getKey());
        assertEquals("p-v1", result[0].getValue());
        // k2 orphan from priority
        assertEquals("k2", result[1].getKey());
        assertEquals("p-v2", result[1].getValue());
        // k3 orphan from fallback
        assertEquals("k3", result[2].getKey());
        assertEquals("f-v3", result[2].getValue());
    }

    // ============================================================
    //  mergeDataParamsDTO — arrays wrapper
    // ============================================================

    @Test
    void mergeDataParamsDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeDataParamsDTO(null, null));
    }

    @Test
    void mergeDataParamsDTO_mergesAllThreeArrays() {
        DataParamsDTO priority = new DataParamsDTO();
        priority.setDataIn(new DataParamDTO[]{new DataParamDTO("k1", "p-in", "pd1")});
        priority.setDataOut(new DataParamDTO[]{new DataParamDTO("k2", "p-out", "pd2")});
        priority.setDataInOut(new DataParamDTO[]{new DataParamDTO("k3", "p-inout", "pd3")});

        DataParamsDTO fallback = new DataParamsDTO();
        fallback.setDataIn(new DataParamDTO[]{new DataParamDTO("k1", "f-in", "fd1")});
        fallback.setDataOut(new DataParamDTO[]{new DataParamDTO("k2", "f-out", "fd2")});
        fallback.setDataInOut(new DataParamDTO[]{new DataParamDTO("k3", "f-inout", "fd3")});

        DataParamsDTO result = mapper.mergeDataParamsDTO(priority, fallback);
        assertEquals("p-in", result.getDataIn()[0].getValue());
        assertEquals("p-out", result.getDataOut()[0].getValue());
        assertEquals("p-inout", result.getDataInOut()[0].getValue());
    }

    // ============================================================
    //  mergeBusinessLogDTO — inherits DataParamsDTO merge
    // ============================================================

    @Test
    void mergeBusinessLogDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeBusinessLogDTO(null, null));
    }

    @Test
    void mergeBusinessLogDTO_priorityTakesPrecedence() {
        BusinessLogDTO priority = new BusinessLogDTO();
        priority.setGroup("p-g");
        priority.setCode("p-c");
        priority.setDescription("p-desc");
        priority.setDefaultDescription("p-dd");
        priority.setValue("p-v");
        priority.setDefaultValue("p-dv");
        priority.setException("p-ex");
        priority.setDefaultException("p-dex");
        priority.setMode(Mode.STATIC);

        BusinessLogDTO fallback = new BusinessLogDTO();
        fallback.setGroup("f-g");
        fallback.setCode("f-c");
        fallback.setDescription("f-desc");
        fallback.setDefaultDescription("f-dd");
        fallback.setValue("f-v");
        fallback.setDefaultValue("f-dv");
        fallback.setException("f-ex");
        fallback.setDefaultException("f-dex");
        fallback.setMode(Mode.DYNAMIC);

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals("p-g", result.getGroup());
        assertEquals("p-c", result.getCode());
        assertEquals("p-desc", result.getDescription());
        assertEquals("p-dd", result.getDefaultDescription());
        assertEquals("p-v", result.getValue());
        assertEquals("p-dv", result.getDefaultValue());
        assertEquals("p-ex", result.getException());
        assertEquals("p-dex", result.getDefaultException());
        assertEquals(Mode.STATIC, result.getMode());
    }

    @Test
    void mergeBusinessLogDTO_fallbackFillsNulls() {
        BusinessLogDTO priority = new BusinessLogDTO();
        priority.setGroup(null);
        priority.setValue("p-v");

        BusinessLogDTO fallback = new BusinessLogDTO();
        fallback.setGroup("f-g");
        fallback.setValue(null);

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals("f-g", result.getGroup());
        assertEquals("p-v", result.getValue());
    }

    @Test
    void mergeBusinessLogDTO_inheritsDataParamsMerge() {
        BusinessLogDTO priority = new BusinessLogDTO();
        priority.setDataIn(new DataParamDTO[]{new DataParamDTO("k", "p-v", "p-d")});

        BusinessLogDTO fallback = new BusinessLogDTO();
        fallback.setDataIn(new DataParamDTO[]{new DataParamDTO("k", "f-v", "f-d")});

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals("p-v", result.getDataIn()[0].getValue());
    }

    @Test
    void mergeBusinessLogDTO_inheritsDataParamsWithPriorityOrphans() {
        BusinessLogDTO priority = new BusinessLogDTO();
        priority.setDataIn(new DataParamDTO[]{
                new DataParamDTO("k1", "p-v1", "p-d1"),
                new DataParamDTO("k2", "p-v2", "p-d2")
        });

        BusinessLogDTO fallback = new BusinessLogDTO();
        fallback.setDataIn(new DataParamDTO[]{
                new DataParamDTO("k1", "f-v1", "f-d1")
        });

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals(2, result.getDataIn().length);
        assertEquals("p-v1", result.getDataIn()[0].getValue());
        assertEquals("p-v2", result.getDataIn()[1].getValue());
    }

    // ============================================================
    //  mergeAuditTrailDTO — inherits BusinessLogDTO merge
    // ============================================================

    @Test
    void mergeAuditTrailDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeAuditTrailDTO(null, null));
    }

    @Test
    void mergeAuditTrailDTO_priorityTakesPrecedence() {
        AuditTrailDTO priority = new AuditTrailDTO();
        priority.setFlowCode("p-flow");
        priority.setGroup("p-g");
        priority.setCode("p-c");
        priority.setMode(Mode.MERGED);

        AuditTrailDTO fallback = new AuditTrailDTO();
        fallback.setFlowCode("f-flow");
        fallback.setGroup("f-g");
        fallback.setCode("f-c");
        fallback.setMode(Mode.DYNAMIC);

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-flow", result.getFlowCode());
        assertEquals("p-g", result.getGroup());
        assertEquals("p-c", result.getCode());
        assertEquals(Mode.MERGED, result.getMode());
    }

    @Test
    void mergeAuditTrailDTO_inheritsBusinessMerge() {
        AuditTrailDTO priority = new AuditTrailDTO();
        priority.setFlowCode("p-flow");
        priority.setValue("p-v");
        priority.setDescription("p-desc");

        AuditTrailDTO fallback = new AuditTrailDTO();
        fallback.setFlowCode("f-flow");
        fallback.setValue("f-v");
        fallback.setDescription("f-desc");

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-flow", result.getFlowCode());
        assertEquals("p-v", result.getValue());
        assertEquals("p-desc", result.getDescription());
    }

    @Test
    void mergeAuditTrailDTO_inheritsDataParamsMerge() {
        AuditTrailDTO priority = new AuditTrailDTO();
        priority.setDataIn(new DataParamDTO[]{new DataParamDTO("k", "p-v", "p-d")});

        AuditTrailDTO fallback = new AuditTrailDTO();
        fallback.setDataIn(new DataParamDTO[]{new DataParamDTO("k", "f-v", "f-d")});

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-v", result.getDataIn()[0].getValue());
    }

    @Test
    void mergeAuditTrailDTO_fallbackFillsNullsInherited() {
        AuditTrailDTO priority = new AuditTrailDTO();
        priority.setFlowCode(null);
        priority.setDefaultDescription(null);

        AuditTrailDTO fallback = new AuditTrailDTO();
        fallback.setFlowCode("f-flow");
        fallback.setDefaultDescription("f-dd");

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("f-flow", result.getFlowCode());
        assertEquals("f-dd", result.getDefaultDescription());
    }
}
