package mx.bastekor.flowweaver.functional.microservices.payments_service.dao;

import mx.bastekor.flowweaver.functional.microservices.payments_service.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransactionDAO {

    private final Map<String, Transaction> store = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        store.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    public Transaction findById(String id) {
        return store.get(id);
    }
}
