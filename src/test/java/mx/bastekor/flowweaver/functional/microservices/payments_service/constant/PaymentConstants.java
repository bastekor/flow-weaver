package mx.bastekor.flowweaver.functional.microservices.payments_service.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentConstants {

    public static final String CURRENCY_MXN = "MXN";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public enum BeneficiaryType {
        OWN_CARD,
        THIRD_PARTY_SAME_BANK,
        THIRD_PARTY_OTHER_BANK,
        SERVICE
    }

    public enum PaymentOption {
        MINIMUM,
        NO_INTEREST,
        OTHER_AMOUNT
    }
}
