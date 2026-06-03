package mx.bastekor.flowweaver.functional.microservices.payments_service.transformer;

import mx.bastekor.flowweaver.functional.microservices.payments_service.model.Transaction;
import mx.bastekor.flowweaver.functional.microservices.payments_service.util.PaymentUtil;

public class PaymentTransformer {

    public static Transaction enrichWithAuth(Transaction transaction) {
        transaction.setAuthorizationCode(PaymentUtil.generateAuthorizationCode());
        transaction.setStatus("COMPLETED");
        return transaction;
    }
}
