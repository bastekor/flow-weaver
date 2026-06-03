package mx.bastekor.flowweaver.functional.microservices.payments_service.service;

import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentResponse;
import mx.bastekor.flowweaver.functional.microservices.payments_service.gateway.InterbankGateway;
import mx.bastekor.flowweaver.functional.microservices.payments_service.gateway.NotificationGateway;
import mx.bastekor.flowweaver.functional.microservices.payments_service.model.Transaction;
import mx.bastekor.flowweaver.functional.microservices.payments_service.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Autowired
    private ClientService clientService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InterbankGateway interbankGateway;
    @Autowired
    private NotificationGateway notificationGateway;
    @Autowired
    private AuditService auditService;

    @BusinessLog(
            group = "PAYMENTS",
            code = "EXECUTE-PAYMENT",
            defaultDescription = "Ejecución de pago bancario",
            defaultValue = "Pago procesado exitosamente"
    )
    public PaymentResponse pay(PaymentRequest request) {
        clientService.validate(request.getClientId());
        accountService.verify(request.getSourceAccount());
        accountService.verifyBalance(request.getSourceAccount(), request.getAmount());

        Transaction debitTx = null;

        switch (request.getBeneficiaryType()) {
            case OWN_CARD:
                accountService.verifyOwnCard(request.getTargetCard());
                debitTx = accountRepository.debit(request);
                accountRepository.credit(request);
                break;
            case THIRD_PARTY_SAME_BANK:
                accountService.verifyThirdParty(request.getTargetCard());
                debitTx = accountRepository.debit(request);
                accountRepository.credit(request);
                break;
            case THIRD_PARTY_OTHER_BANK:
                accountService.verifyThirdParty(request.getTargetCard());
                interbankGateway.validate(request.getTargetCard());
                debitTx = accountRepository.debit(request);
                interbankGateway.transfer(request.getTargetCard(), request.getAmount());
                break;
            case SERVICE:
                accountService.verifyService(request.getServiceId());
                debitTx = accountRepository.debitService(request);
                break;
        }

        notificationGateway.sendEmail(request);
        notificationGateway.sendSMS(request);

        String txId = debitTx != null ? debitTx.getTransactionId() : "UNKNOWN";
        String authCode = debitTx != null ? debitTx.getAuthorizationCode() : "NONE";
        auditService.audit("PAYMENT", txId);

        return new PaymentResponse(
                txId,
                "COMPLETED",
                "Payment processed successfully",
                authCode,
                request.getAmount()
        );
    }
}
