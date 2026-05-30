package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.enums.Mode;
import org.junit.jupiter.api.Test;

import static mx.bastekor.flowweaver.factory.DtoFactory.createDataParamDTO;
import static org.junit.jupiter.api.Assertions.*;

class UtilMapperTest {

    private final UtilMapper mapper = new UtilMapperImpl();

    @Test
    void mergeDataParamDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeDataParamDTO(null, null));
    }

    @Test
    void mergeDataParamDTO_priorityTakesPrecedence() {
        DataParamDTO priority = createDataParamDTO("k", "p-v", "p-d");
        DataParamDTO fallback = createDataParamDTO("k", "f-v", "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("k", result.getKey());
        assertEquals("p-v", result.getValue());
        assertEquals("p-d", result.getDefaultValue());
    }

    @Test
    void mergeDataParamDTO_priorityPartialFallbackFills() {
        DataParamDTO priority = createDataParamDTO("k", "p-v", null);
        DataParamDTO fallback = createDataParamDTO("k", null, "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("p-v", result.getValue());
        assertEquals("f-d", result.getDefaultValue());
    }

    @Test
    void mergeDataParamDTO_priorityBlankFallbackFills() {
        DataParamDTO priority = createDataParamDTO("k", "", "");
        DataParamDTO fallback = createDataParamDTO("k", "f-v", "f-d");
        DataParamDTO result = mapper.mergeDataParamDTO(priority, fallback);
        assertEquals("f-v", result.getValue());
        assertEquals("f-d", result.getDefaultValue());
    }

    @Test
    void mergeDataParamDTOArrays_bothNull_returnsEmptyArray() {
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(null, null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_priorityNull_returnsFilteredFallback() {
        DataParamDTO[] fallback = {createDataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(null, fallback);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackNull_returnsFilteredPriority() {
        DataParamDTO[] priority = {createDataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, null);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_priorityEmpty_returnsFilteredFallback() {
        DataParamDTO[] fallback = {createDataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(new DataParamDTO[0], fallback);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackEmpty_returnsFilteredPriority() {
        DataParamDTO[] priority = {createDataParamDTO("k", "v", "d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_matchingKeys_mergesProperties() {
        DataParamDTO[] priority = {createDataParamDTO("k1", "p-v", "p-d")};
        DataParamDTO[] fallback = {createDataParamDTO("k1", "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(1, result.length);
        assertEquals("p-v", result[0].getValue());
        assertEquals("p-d", result[0].getDefaultValue());
    }

    @Test
    void mergeDataParamDTOArrays_matchingKeys_priorityBlankUsesFallback() {
        DataParamDTO[] priority = {createDataParamDTO("k1", "", null)};
        DataParamDTO[] fallback = {createDataParamDTO("k1", "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals("f-v", result[0].getValue());
        assertEquals("f-d", result[0].getDefaultValue());
    }

    @Test
    void mergeDataParamDTOArrays_priorityOrphansKept() {
        DataParamDTO[] priority = {createDataParamDTO("k1", "v1", "d1"), createDataParamDTO("k2", "v2", "d2")};
        DataParamDTO[] fallback = {createDataParamDTO("k3", "v3", "d3")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(3, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("k2", result[1].getKey());
        assertEquals("k3", result[2].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_fallbackOrphansAdded() {
        DataParamDTO[] priority = {createDataParamDTO("k1", "p-v", "p-d")};
        DataParamDTO[] fallback = {createDataParamDTO("k1", "f-v", "f-d"), createDataParamDTO("k2", "f-v2", "f-d2")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(2, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("p-v", result[0].getValue());
        assertEquals("k2", result[1].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_nullKeyDiscarded() {
        DataParamDTO[] priority = {createDataParamDTO(null, "v1", "d1"), createDataParamDTO("k", "v2", "d2")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
    }

    @Test
    void mergeDataParamDTOArrays_blankKeyDiscarded_returnsEmptyArray() {
        DataParamDTO[] priority = {createDataParamDTO(" ", "v1", "d1")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_emptyKeyDiscarded_returnsEmptyArray() {
        DataParamDTO[] priority = {createDataParamDTO("", "v1", "d1")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, new DataParamDTO[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void mergeDataParamDTOArrays_nullKeyInFallbackIgnored() {
        DataParamDTO[] priority = {createDataParamDTO("k", "p-v", "p-d")};
        DataParamDTO[] fallback = {createDataParamDTO(null, "f-v", "f-d")};
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);
        assertEquals(1, result.length);
        assertEquals("k", result[0].getKey());
        assertEquals("p-v", result[0].getValue());
    }

    @Test
    void mergeDataParamDTOArrays_mixedScenario() {
        DataParamDTO[] priority = {
                createDataParamDTO("k1", "p-v1", "p-d1"),
                createDataParamDTO("k2", "p-v2", "p-d2"),
                createDataParamDTO(null, "p-v3", null),
                createDataParamDTO("", "p-v4", null)
        };
        DataParamDTO[] fallback = {
                createDataParamDTO("k1", "f-v1", "f-d1"),
                createDataParamDTO("k3", "f-v3", "f-d3"),
                createDataParamDTO("  ", "f-v4", null)
        };
        DataParamDTO[] result = mapper.mergeDataParamDTOArrays(priority, fallback);

        assertEquals(3, result.length);
        assertEquals("k1", result[0].getKey());
        assertEquals("p-v1", result[0].getValue());
        assertEquals("k2", result[1].getKey());
        assertEquals("p-v2", result[1].getValue());
        assertEquals("k3", result[2].getKey());
        assertEquals("f-v3", result[2].getValue());
    }

    @Test
    void mergeDataParamsDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeDataParamsDTO(null, null));
    }

    @Test
    void mergeDataParamsDTO_mergesAllThreeArrays() {
        DataParamsDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createDataParamsDTO(
                new DataParamDTO[]{createDataParamDTO("k1", "p-in", "pd1")},
                new DataParamDTO[]{createDataParamDTO("k2", "p-out", "pd2")},
                new DataParamDTO[]{createDataParamDTO("k3", "p-inout", "pd3")}
        );
        DataParamsDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createDataParamsDTO(
                new DataParamDTO[]{createDataParamDTO("k1", "f-in", "fd1")},
                new DataParamDTO[]{createDataParamDTO("k2", "f-out", "fd2")},
                new DataParamDTO[]{createDataParamDTO("k3", "f-inout", "fd3")}
        );

        DataParamsDTO result = mapper.mergeDataParamsDTO(priority, fallback);
        assertEquals("p-in", result.getDataIn()[0].getValue());
        assertEquals("p-out", result.getDataOut()[0].getValue());
        assertEquals("p-inout", result.getDataInOut()[0].getValue());
    }

    @Test
    void mergeBusinessLogDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeBusinessLogDTO(null, null));
    }

    @Test
    void mergeBusinessLogDTO_priorityTakesPrecedence() {
        BusinessLogDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d -> {
            d.setGroup("p-g");
            d.setCode("p-c");
            d.setDescription("p-desc");
            d.setDefaultDescription("p-dd");
            d.setValue("p-v");
            d.setDefaultValue("p-dv");
            d.setException("p-ex");
            d.setDefaultException("p-dex");
            d.setMode(Mode.STATIC);
        });
        BusinessLogDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d -> {
            d.setGroup("f-g");
            d.setCode("f-c");
            d.setDescription("f-desc");
            d.setDefaultDescription("f-dd");
            d.setValue("f-v");
            d.setDefaultValue("f-dv");
            d.setException("f-ex");
            d.setDefaultException("f-dex");
            d.setMode(Mode.DYNAMIC);
        });

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
        BusinessLogDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d -> {
            d.setGroup(null);
            d.setValue("p-v");
        });
        BusinessLogDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d -> {
            d.setGroup("f-g");
            d.setValue(null);
        });

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals("f-g", result.getGroup());
        assertEquals("p-v", result.getValue());
    }

    @Test
    void mergeBusinessLogDTO_inheritsDataParamsMerge() {
        BusinessLogDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d ->
                d.setDataIn(new DataParamDTO[]{createDataParamDTO("k", "p-v", "p-d")}));
        BusinessLogDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d ->
                d.setDataIn(new DataParamDTO[]{createDataParamDTO("k", "f-v", "f-d")}));

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals("p-v", result.getDataIn()[0].getValue());
    }

    @Test
    void mergeBusinessLogDTO_inheritsDataParamsWithPriorityOrphans() {
        BusinessLogDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d ->
                d.setDataIn(new DataParamDTO[]{
                        createDataParamDTO("k1", "p-v1", "p-d1"),
                        createDataParamDTO("k2", "p-v2", "p-d2")
                }));
        BusinessLogDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createBusinessLogDTO(d ->
                d.setDataIn(new DataParamDTO[]{
                        createDataParamDTO("k1", "f-v1", "f-d1")
                }));

        BusinessLogDTO result = mapper.mergeBusinessLogDTO(priority, fallback);
        assertEquals(2, result.getDataIn().length);
        assertEquals("p-v1", result.getDataIn()[0].getValue());
        assertEquals("p-v2", result.getDataIn()[1].getValue());
    }

    @Test
    void mergeAuditTrailDTO_bothNull_returnsNull() {
        assertNull(mapper.mergeAuditTrailDTO(null, null));
    }

    @Test
    void mergeAuditTrailDTO_priorityTakesPrecedence() {
        AuditTrailDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode("p-flow");
            d.setGroup("p-g");
            d.setCode("p-c");
            d.setMode(Mode.MERGED);
        });
        AuditTrailDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode("f-flow");
            d.setGroup("f-g");
            d.setCode("f-c");
            d.setMode(Mode.DYNAMIC);
        });

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-flow", result.getFlowCode());
        assertEquals("p-g", result.getGroup());
        assertEquals("p-c", result.getCode());
        assertEquals(Mode.MERGED, result.getMode());
    }

    @Test
    void mergeAuditTrailDTO_inheritsBusinessMerge() {
        AuditTrailDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode("p-flow");
            d.setValue("p-v");
            d.setDescription("p-desc");
        });
        AuditTrailDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode("f-flow");
            d.setValue("f-v");
            d.setDescription("f-desc");
        });

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-flow", result.getFlowCode());
        assertEquals("p-v", result.getValue());
        assertEquals("p-desc", result.getDescription());
    }

    @Test
    void mergeAuditTrailDTO_inheritsDataParamsMerge() {
        AuditTrailDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d ->
                d.setDataIn(new DataParamDTO[]{createDataParamDTO("k", "p-v", "p-d")}));
        AuditTrailDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d ->
                d.setDataIn(new DataParamDTO[]{createDataParamDTO("k", "f-v", "f-d")}));

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("p-v", result.getDataIn()[0].getValue());
    }

    @Test
    void mergeAuditTrailDTO_fallbackFillsNullsInherited() {
        AuditTrailDTO priority = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode(null);
            d.setDefaultDescription(null);
        });
        AuditTrailDTO fallback = mx.bastekor.flowweaver.factory.DtoFactory.createAuditTrailDTO(d -> {
            d.setFlowCode("f-flow");
            d.setDefaultDescription("f-dd");
        });

        AuditTrailDTO result = mapper.mergeAuditTrailDTO(priority, fallback);
        assertEquals("f-flow", result.getFlowCode());
        assertEquals("f-dd", result.getDefaultDescription());
    }
}
