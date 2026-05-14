package mx.bastekor.flowweaver.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.model.SafeSerializer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafeSnapshotMapperTest {

    @Test
    void inspectStringArgOutput() throws Exception {
        Method method = TestService.class.getMethod("greet", String.class, String.class, int.class);
        String json = SafeSnapshotMapper.mapArgs(method, new Object[]{"Hello World", null, 42}, 5);
        System.out.println("=== JSON OUTPUT ===");
        System.out.println(json);
        System.out.println("=== END ===");
        assertNotNull(json);
        assertTrue(json.contains("Hello World"), "JSON should contain the string value");
        assertTrue(json.contains("_string"), "JSON should contain _string field");
        assertTrue(json.contains("value"), "JSON should contain value field");
    }

    @Test
    void inspectComplexArgsOutput() throws Exception {
        Method method = TestService.class.getMethod("process", String.class, Person.class);
        Person p = new Person("Juan", "juan@email.com", new Money("USD", java.math.BigDecimal.valueOf(100)));
        String json = SafeSnapshotMapper.mapArgs(method, new Object[]{"test", p}, 3);
        System.out.println("=== COMPLEX JSON OUTPUT ===");
        System.out.println(json);
        System.out.println("=== END ===");
        assertNotNull(json);
    }

    static class TestService {
        @BusinessLog
        public String greet(String saludo, String nombre, int cantidad) {
            return "OK";
        }

        @BusinessLog
        public String process(String code, Person person) {
            return "OK";
        }
    }

    @Getter
    @AllArgsConstructor
    static class Person {
        private String name;
        private String email;
        private Money amount;
    }

    @Getter
    @AllArgsConstructor
    static class Money {
        private String currency;
        private java.math.BigDecimal amount;
    }
}
