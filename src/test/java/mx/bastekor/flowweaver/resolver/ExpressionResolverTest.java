package mx.bastekor.flowweaver.resolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpressionResolverTest {

    private static String json(String resource) {
        try {
            var is = Objects.requireNonNull(
                    ExpressionResolverTest.class.getResourceAsStream(resource),
                    "Resource not found: " + resource);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + resource, e);
        }
    }

    private static final String MC = json("/snapshots/method-contract.jsonc");
    private static final String SP = json("/snapshots/simple-primitives.json");
    private static final String DN = json("/snapshots/deep-nested.json");
    private static final String MT = json("/snapshots/mixed-types.json");
    private static final String FLAT = json("/snapshots/flat-data.json");
    private static final String CHAIN = json("/snapshots/chain-10.json");
    private static final String TREE = json("/snapshots/tree-7.json");
    private static final String MATRIX = json("/snapshots/matrix-3d.json");
    private static final String MAPS = json("/snapshots/maps-nested.json");
    private static final String PROF = json("/snapshots/profiles.json");

    // ===================================================================
    //  chain-10.json — 10 niveles de objetos anidados
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "l0.l1.l2.l3.l4.l5.l6.l7.l8.l9.value, done",
            "l0.l1.l2.l3.l4.l5.l6.l7.l8.l9,       __NULL__",
    })
    void chain10(String expr, String expected) {
        asserts(CHAIN, expr, expected);
    }

    // ===================================================================
    //  tree-7.json — árbol binario de 7 niveles
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "root.left.left.left.left.left.left.data, deep-left",
            "root.left.left.left.left.left.right.data, deep-right",
            "root.left.left.left.right.data,           llr-data",
            "root.left.right.left.data,                rl-data",
            "root.right.left.data,                     r-root",
    })
    void tree7(String expr, String expected) {
        asserts(TREE, expr, expected);
    }

    // ===================================================================
    //  matrix-3d.json — arrays 3D + tags + metadatos
    // ===================================================================

    // índices positivos
    @ParameterizedTest
    @CsvSource({
            "grid[0][0][0], a00",
            "grid[0][1][2], a12",
            "grid[1][2][1], b21",
            "tags[0],       alpha",
            "tags[4],       epsilon",
            "metadata.dimensions[0], 2",
            "metadata.dimensions[-1], 3",
            "metadata.labels.first,   matrix-A",
    })
    void matrix3d(String expr, String expected) {
        asserts(MATRIX, expr, expected);
    }

    // índices negativos
    @ParameterizedTest
    @CsvSource({
            "tags[-1],    epsilon",
            "tags[-2],    delta",
            "tags[-5],    alpha",
            "grid[-1][0][0], b00",
            "grid[-1][-1][-1], b22",
    })
    void matrixNegative(String expr, String expected) {
        asserts(MATRIX, expr, expected);
    }

    // sintaxis alternativa: .0 en vez de [0], args/arg patterns
    @ParameterizedTest
    @CsvSource({
            "grid.0.0.0,       a00",
            "tags.args0,       alpha",
            "tags.arg0,        alpha",
            "tags.args_4,      epsilon",
            "tags.arg-1,       epsilon",
            "tags.args[1],     beta",
            "tags.arg[-2],     delta",
            "tags.args-2,      delta",
            "tags.arg_3,       delta",
    })
    void matrixAltSyntax(String expr, String expected) {
        asserts(MATRIX, expr, expected);
    }

    // ===================================================================
    //  maps-nested.json — keys con puntos y caracteres especiales
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "config.simple,                 value",
            "config[\"with.dot\"],           dot-value",
            "config['with space'],          space-value",
            "config[\"with.special.chars!\"], special-value",
            "config[\"nested.map\"][\"inner.key\"], inner-value",
            "config[\"nested.map\"].plain,   plain-value",
            "lookup[\"my.key.with.dots\"].role, admin",
            "lookup[\"my.key.with.dots\"].level, 5",
            "mixed.plain,                   plain-field",
            "mixed[\"data[0]\"],             bracket-field",
            "mixed.args,                    args-field",
    })
    void mapsNested(String expr, String expected) {
        asserts(MAPS, expr, expected);
    }

    // key con punto interno (deep.map)
    @Test
    void mapsDeepDotKey() {
        assertEquals("reached",
                ExpressionResolver.resolve(MAPS, "[\"deep.map\"][\"also.nested\"][\"finally\"].value"));
    }

    // key sin comillas
    @Test
    void mapsBareKey() {
        assertEquals("dot-value", ExpressionResolver.resolve(MAPS, "config[with.dot]"));
    }

    // ===================================================================
    //  profiles.json — objetos + arrays + maps (estructura mixta)
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "users[0].name,                Alice",
            "users[1].name,                Bob",
            "users[-1].name,               Bob",
            "users[0].emails[0],           alice@work.com",
            "users[0].emails[-1],          alice@home.com",
            "users[0].addresses[0].city,   New York",
            "users[0].addresses[-1].city,  Boston",
            "users[1].addresses[0].city,   Chicago",
            "users[0].roles[\"admin\"].level,  3",
            "users[0].roles[\"editor\"].since, 2024",
            "users[-1].roles[\"viewer\"].level, 1",
            "summary.total,                2",
            "summary.cities[0],            New York",
            "summary.cities[-1],           Chicago",
    })
    void profiles(String expr, String expected) {
        asserts(PROF, expr, expected);
    }

    // ===================================================================
    //  flat-data.json — estructura plana sin wrappers
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "app,                FlowWeaver",
            "version,            0.1.1",
            "environment,        development",
            "features.logging,   true",
            "features.audit,     false",
            "features.maxDepth,  3",
            "tags[0],            beta",
            "tags[-1],           experimental",
    })
    void flatData(String expr, String expected) {
        asserts(FLAT, expr, expected);
    }

    @Test
    void flatDataNullField() {
        assertNull(ExpressionResolver.resolve(FLAT, "owner"));
    }

    // ===================================================================
    //  Snapshots con SafeSerializer (expresiones explícitas con _value)
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "_args[0]._value,    test",
            "_args.0._toString,  test",
            "_args.0._type,      java.lang.String",
            "_args.1._value[1]._value.amount._value.currency._value.__NULL__, __NULL__",
    })
    void methodContract(String expr, String expected) {
        asserts(MC, expr, expected);
    }

    @Test
    void methodContractNestedCurrency() {
        String result = ExpressionResolver.resolve(MC,
                "_args[1]._value[1]._value.amount._value.currency._toString");
        assertEquals("USD", result);
    }

    @Test
    void methodContractPersonName() {
        String result = ExpressionResolver.resolve(MC,
                "_args[1]._value[0]._value.name");
        assertEquals("Juan", result);
    }

    @ParameterizedTest
    @CsvSource({
            "_args.0._value,   john_doe",
            "_args.1._value,   25",
            "_args.2._value,   true",
            "_args.3._toString, 75000.50",
            "_args.4._value, __EMPTY__",
    })
    void simplePrimitives(String expr, String expected) {
        asserts(SP, expr, expected);
    }

    // ===================================================================
    //  deep-nested.json — anidación profunda con wrappers SafeSerializer
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "_args[0]._value,                            ORD-001",
            "_args[0]._toString,                         ORD-001",
            "_args[0]._type,                             java.lang.String",
            "_args[1]._value.name._toString,             Alice",
            "_args[1]._value.tier._toString,             gold",
            "_args[1]._value.addresses._value[0]._value.street._toString, 123 Main St",
            "_args[1]._value.addresses._value[0]._value.city._toString,    Springfield",
            "_args[1]._value.addresses._value[1]._value.street._toString, 456 Oak Ave",
            "_args[1]._value.addresses._value[-1]._value.city._toString,   Metropolis",
            "_args[1]._value.addresses._value[99],                         __NULL__",
    })
    void deepNested(String expr, String expected) {
        asserts(DN, expr, expected);
    }

    @Test
    void deepNestedNonExistent() {
        assertNull(ExpressionResolver.resolve(DN, "no.existe"));
    }

    // ===================================================================
    //  mixed-types.json — tipos mezclados con wrappers SafeSerializer
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "_args[0]._value,                            42",
            "_args[0]._toString,                         42",
            "_args[0]._type,                             long",
            "_args[1]._toString,                         __EMPTY__",
            "_args[1]._value,                            __EMPTY__",
            "_args[2]._value[0]._toString,               urgent",
            "_args[2]._value[1]._toString,               backend",
            "_args[2]._value[2]._toString,               java",
            "_args[2]._value[-1]._toString,              java",
            "_args[3]._value.timeout._toString,          5000",
            "_args[3]._value.retries._toString,          3",
            "_args[3]._value.flags._value.debug._toString, true",
            "_args[3]._value.flags._value.cache._toString, false",
    })
    void mixedTypes(String expr, String expected) {
        asserts(MT, expr, expected);
    }

    @Test
    void mixedTypesNoArgs() {
        assertNull(ExpressionResolver.resolve(MT, "_args[99]"));
    }

    // ===================================================================
    //  Scope explícito
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "json, app,               FlowWeaver",
            "json, features.logging,  true",
            "tags, 0,                 beta",
            "tags, -1,                experimental",
    })
    void scopeFlat(String scope, String expr, String expected) {
        asserts(FLAT, scope, expr, expected);
    }

    @ParameterizedTest
    @CsvSource({
            "metadata.labels, first,  matrix-A",
    })
    void scopeMatrix(String scope, String expr, String expected) {
        asserts(MATRIX, scope, expr, expected);
    }

    @ParameterizedTest
    @CsvSource({
            "users, 0.name,           Alice",
            "users, -1.name,          Bob",
            "users[0].roles, admin.level, 3",
    })
    void scopeProfiles(String scope, String expr, String expected) {
        asserts(PROF, scope, expr, expected);
    }

    @Test
    void scopeMapsDotKey() {
        assertEquals("inner-value",
                ExpressionResolver.resolve(MAPS, "config", "[\"nested.map\"][\"inner.key\"]"));
    }

    @Test
    void scopeMapsAltSyntax() {
        assertEquals("plain-value",
                ExpressionResolver.resolve(MAPS, "config", "[\"nested.map\"].plain"));
    }

    @Test
    void scopeNull() {
        assertEquals("FlowWeaver", ExpressionResolver.resolve(FLAT, null, "app"));
    }

    @Test
    void scopeEmpty() {
        assertEquals("FlowWeaver", ExpressionResolver.resolve(FLAT, "", "app"));
    }

    @Test
    void scopeNonExistent() {
        assertNull(ExpressionResolver.resolve(FLAT, "no.existe", "algo"));
    }

    // ===================================================================
    //  Edge cases
    // ===================================================================

    @ParameterizedTest
    @CsvSource({
            "no.existe.campo",
            "_args.99",
            "users[99]",
            "tags[-99]",
            "tags.nonexistent",
            "config.\"\"",
    })
    void nonExistentPath(String expr) {
        assertNull(ExpressionResolver.resolve(FLAT, expr));
    }

    @Test
    void nullJson() {
        assertNull(ExpressionResolver.resolve(null, "app"));
    }

    @Test
    void nullExpression() {
        assertNull(ExpressionResolver.resolve(FLAT, null));
    }

    @Test
    void blankExpression() {
        assertNull(ExpressionResolver.resolve(FLAT, "  "));
    }

    @Test
    void invalidJson() {
        assertNull(ExpressionResolver.resolve("not json", "app"));
    }

    @Test
    void bracketOnlyEmpty() {
        assertNull(ExpressionResolver.resolve(MATRIX, "tags[]"));
    }

    // ===================================================================
    //  Integración con SafeSnapshotMapper real
    // ===================================================================

    @Test
    void integrationRealSnapshot() throws Exception {
        var method = TestService.class.getMethod("greet", String.class);
        String snap = mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs(method, new Object[]{"Hello"}, 3);
        assertEquals("Hello", ExpressionResolver.resolve(snap, "_args[0]._value"));
    }

    @Test
    void integrationScope() throws Exception {
        var method = TestService.class.getMethod("greet", String.class);
        String snap = mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs(method, new Object[]{"World"}, 3);
        assertEquals("World", ExpressionResolver.resolve(snap, "_args[0]", "_value"));
    }

    static class TestService {
        public String greet(String name) {
            return "OK";
        }
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    private static void asserts(String json, String expr, String expected) {
        String result = ExpressionResolver.resolve(json, expr);
        if ("__NULL__".equals(expected)) {
            assertNull(result, expr);
        } else if ("__EMPTY__".equals(expected)) {
            assertEquals("", result, expr);
        } else {
            assertEquals(expected, result, expr);
        }
    }

    private static void asserts(String json, String scope, String expr, String expected) {
        String msg = scope + " | " + expr;
        String result = ExpressionResolver.resolve(json, scope, expr);
        if ("__NULL__".equals(expected)) {
            assertNull(result, msg);
        } else if ("__EMPTY__".equals(expected)) {
            assertEquals("", result, msg);
        } else {
            assertEquals(expected, result, msg);
        }
    }
}
