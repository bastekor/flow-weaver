package mx.bastekor.flowweaver.functional.microservices.payments_service.repository;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dao.TransactionDAO;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import mx.bastekor.flowweaver.functional.microservices.payments_service.mapper.PaymentMapper;
import mx.bastekor.flowweaver.functional.microservices.payments_service.model.Transaction;
import mx.bastekor.flowweaver.functional.microservices.payments_service.transformer.PaymentTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountRepository {

    @Autowired
    private TransactionDAO transactionDAO;

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "DEBIT-ACCOUNT",
            defaultDescription = "Aplicando cargo a cuenta de origen",
            defaultValue = "Cargo aplicado correctamente"
    )
    public Transaction debit(PaymentRequest request) {
        Transaction tx = PaymentMapper.toTransaction(request);
        tx.setType("DEBIT");
        PaymentTransformer.enrichWithAuth(tx);
        return transactionDAO.save(tx);
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "CREDIT-ACCOUNT",
            defaultDescription = "Aplicando abono a cuenta destino",
            defaultValue = "Abono aplicado correctamente"
    )
    public Transaction credit(PaymentRequest request) {
        Transaction tx = PaymentMapper.toTransaction(request);
        tx.setType("CREDIT");
        tx.setStatus("COMPLETED");
        return transactionDAO.save(tx);
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "DEBIT-SERVICE",
            defaultDescription = "Aplicando cargo por pago de servicio",
            defaultValue = "Cargo de servicio aplicado correctamente"
    )
    public Transaction debitService(PaymentRequest request) {
        Transaction tx = PaymentMapper.toTransaction(request);
        tx.setType("SERVICE_PAYMENT");
        PaymentTransformer.enrichWithAuth(tx);
        return transactionDAO.save(tx);
    }
}
