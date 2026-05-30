package mx.bastekor.flowweaver.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerFactory {

    public static BusinessLogContainer createBusinessLogContainer() {
        return createBusinessLogContainer("c", "G", "C");
    }

    public static BusinessLogContainer createBusinessLogContainer(String correlationId, String group, String code) {
        return new BusinessLogContainer(correlationId, group, code);
    }

    public static AuditTrailContainer createAuditTrailContainer() {
        AuditTrailContainer c = new AuditTrailContainer();
        c.setGroup("G");
        c.setCode("AT");
        return c;
    }

    public static AuditTrailContainer createAuditTrailContainer(String group, String parentCode, String code) {
        AuditTrailContainer c = new AuditTrailContainer();
        c.setGroup(group);
        c.setParentCode(parentCode);
        c.setCode(code);
        return c;
    }
}
