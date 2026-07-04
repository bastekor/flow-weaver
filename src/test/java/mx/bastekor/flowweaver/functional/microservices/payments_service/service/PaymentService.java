package mx.bastekor.flowweaver.functional.microservices.payments_service.service;

import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
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
            description = "arg[1]",
            defaultDescription = "Ejecución de pago bancario",
            defaultValue = "Pago procesado exitosamente",
            dataOut = {
                    @DataParam(key = "clientId", value = "request.clientId", defaultValue = "Valor del cliente 1"),
                    @DataParam(key = "account", value = "request.sourceAccount", defaultValue = "XXXX-XXXX-XXXX-XXXX"),
                    @DataParam(key = "card", value = "request.targetCard", defaultValue = "YYYY-YYYY-YYYY-YYYY"),
                    @DataParam(key = "amount", value = "request.amount", defaultValue = "$"),
                    @DataParam(key = "currency", value = "request.currency", defaultValue = "$$$"),
                    @DataParam(key = "service", value = "request.serviceId", defaultValue = "paguitos papi"),
            }
    )
    public PaymentResponse pay(PaymentRequest request, String message) {
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
