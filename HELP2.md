# Ayuda

### Firma del método anotado en formato JSON con máximo en profundidad 10.
```json
{
    "_class": "com.example.customer.service.CustomerServiceImpl3",
    "_method": "createCustomer",
    "_returnType": "com.example.customer.dto.CustomerDTO",
    "args": [
        {
            "index": 0,
            "name": "customerDTO",
            "_type": "com.example.customer.model.CustomerDTO",
            "_string": "CustomerDTO(id=1, name=LUIS, email=luis@email.com)",
            "value": {
                "id": "1",
                "name": "LUIS",
                "email": "luis@email.com",
                "address": {
                    "_type": "com.example.Address",
                    "_string": "Address(street=Calle 123, city=CDMX, country=MX)",
                    "value": {
                        "_depth": "limit_reached",
                        "_string": "Address(street=Calle 123, city=CDMX, country=MX)"
                    }
                },
                "notifier": null
            }
        }
    ]
}
```

# Notas
* La clase **ExpressionResolver** será pública, por ende, podrá ser usada por dev