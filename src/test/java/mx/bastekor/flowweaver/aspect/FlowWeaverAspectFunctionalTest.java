package mx.bastekor.flowweaver.aspect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.config.FlowWeaverConfig;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;
import mx.bastekor.flowweaver.service.IFlowWeaverAspectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(classes = FlowWeaverConfig.class, webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
        "flow-weaver.debug.recursive=false",
        "flow-weaver.max-depth=5"
})
class FlowWeaverAspectFunctionalTest {

    @MockBean
    private IFlowWeaverAspectService mockService;

    @Autowired
    private CustomerControllerTest customerController;

    @Autowired
    private CustomerServiceTestImpl customerService;

    @Autowired
    private CustomerRepositoryTest customerRepository;

    @BeforeEach
    void setUp() {
        FlowWeaverContext.clearCurrentThreadContainer();
    }

    @Test
    void tareaAsincrona_happyPath() {
        CustomerDTO cliente1 = new CustomerDTO("Juan Pérez", "juan@email.com");
        CustomerDTO cliente2 = new CustomerDTO("María García", "maria@email.com");
        List<CustomerDTO> customers = List.of(cliente1, cliente2);

        String result = customerController.tareaAsincronaController("iPhone15", customers);

        assertEquals("Completado: tareaAsincronaController", result);

        ArgumentCaptor<BusinessLogContainer> blCaptor = ArgumentCaptor.forClass(BusinessLogContainer.class);
        verify(mockService, times(1)).processBusinessLog(blCaptor.capture());

        ArgumentCaptor<AuditTrailContainer> atCaptor = ArgumentCaptor.forClass(AuditTrailContainer.class);
        verify(mockService, times(12)).processAuditTrail(atCaptor.capture());

        BusinessLogContainer rootBl = blCaptor.getValue();
        assertNotNull(rootBl.getCorrelationId());
        assertEquals("TEST_2", rootBl.getCode());
        assertEquals("PRUEBAS_DE_PRUEBAS", rootBl.getGroup());
        assertEquals(StatusEnum.SOURCE_SUCCESS, rootBl.getStatus());
        assertNotNull(rootBl.getExitSignature());
        assertNotNull(rootBl.getResponse());
        assertEquals("args[0]", rootBl.getBusinessLog().value());
        assertEquals("args[1].0.email", rootBl.getBusinessLog().description());
    }

    @Component
    static class CustomerControllerTest {

        @Autowired
        private CustomerServiceTestImpl customerService;

        @BusinessLog(
                group = "PRUEBAS_DE_PRUEBAS",
                code = "TEST_2",
                value = "args[0]",
                defaultValue = "Dije valor args[0]",
                description = "args[1].0.email",
                defaultDescription = "Dije valor args[1]"
        )
        public String tareaAsincronaController(String param1, List<CustomerDTO> customers) {
            customerService.createCustomer(customers.get(0));
            customerService.venderProducto(param1);
            customerService.comprarProducto(customers);
            customerService.actualizarStock(param1, customers);
            customerService.generarGanancias(param1, customers);
            return "Completado: tareaAsincronaController";
        }
    }

    @Service
    static class CustomerServiceTestImpl {

        @Autowired
        private CustomerRepositoryTest customerRepository;

        @AuditTrail(code = "CREATE_CUSTOMER", parentCode = "TEST_2")
        public CustomerDTO createCustomer(CustomerDTO customer) {
            return customerRepository.save(customer);
        }

        @AuditTrail(code = "SELL_PRODUCT", mode = Mode.DYNAMIC, parentCode = "TEST_2")
        public String venderProducto(String producto) {
            return "Vendido: " + producto;
        }

        @AuditTrail(
                code = "BUY_PRODUCT",
                parentCode = "TEST_2",
                dataIn = {
                        @DataParam(key = "totalClientes", value = "args[0].size()", defaultValue = "0")
                }
        )
        public String comprarProducto(List<CustomerDTO> customers) {
            return "Comprado para " + customers.size() + " clientes";
        }

        @AuditTrail(code = "UPDATE_STOCK", mode = Mode.MERGED, parentCode = "TEST_2")
        public String actualizarStock(String param1, List<CustomerDTO> customers) {
            return "Stock actualizado";
        }

        @AuditTrail(
                code = "GENERATE_PROFITS",
                parentCode = "TEST_2",
                dataOut = {
                        @DataParam(key = "total", value = "response", defaultValue = "0")
                }
        )
        public String generarGanancias(String param1, List<CustomerDTO> customers) {
            return "1000";
        }
    }

    @Component
    static class CustomerRepositoryTest {

        @AuditTrail(
                code = "SAVE_CUSTOMER",
                parentCode = "TEST_2",
                description = "args[0].name",
                defaultDescription = "Guardando cliente"
        )
        public CustomerDTO save(CustomerDTO customer) {
            return customer;
        }
    }

    @Getter
    @AllArgsConstructor
    static class CustomerDTO {
        private String name;
        private String email;
    }
}
