package mx.bastekor.flowweaver.functional.microservices.payments_service.mapper;

import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentResponse;
import mx.bastekor.flowweaver.functional.microservices.payments_service.model.Transaction;
import mx.bastekor.flowweaver.functional.microservices.payments_service.util.PaymentUtil;

import java.time.LocalDateTime;

public class PaymentMapper {

    public static Transaction toTransaction(PaymentRequest request) {
        Transaction tx = new Transaction();
        tx.setTransactionId(PaymentUtil.generateTransactionId());
        tx.setSourceAccount(request.getSourceAccount());
        tx.setTargetAccount(request.getTargetCard());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setType(request.getBeneficiaryType().name());
        tx.setTimestamp(LocalDateTime.now());
        return tx;
    }

    public static PaymentResponse toResponse(Transaction transaction, String message) {
        return new PaymentResponse(
                transaction.getTransactionId(),
                transaction.getStatus(),
                message,
                transaction.getAuthorizationCode(),
                transaction.getAmount()
        );
    }
}
