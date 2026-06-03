package mx.bastekor.flowweaver.resolver;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve expresiones de acceso a datos contra cualquier árbol JSON.
 * <p>
 * Es 100% nativo: no conoce SafeSerializer, wrappers, {@code _args} ni
 * {@code _toString}. Opera exclusivamente sobre la estructura JSON.
 * <p>
 * <b>Sintaxis de segmentos:</b>
 * <pre>
 *   campo              → field lookup en objeto / key de mapa
 *   [n]                → índice positivo de array
 *   [-n]               → índice negativo de array (desde el final)
 *   [texto]            → key literal de mapa (sin comillas)
 *   ["texto"]          → key literal de mapa (con comillas dobles)
 *   ['texto']          → key literal de mapa (con comillas simples)
 *   n                  → número desnudo como índice de array
 * </pre>
 * <p>
 * Los segmentos se separan por punto ({@code .}). Un segmento puede
 * contener {@code campo[índice]} y se expande automáticamente a dos
 * pasos de navegación. No existe auto-unwrap ni lógica de terceros.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExpressionResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---------------------------------------------------------------
    //  API pública — original (firmas intactas)
    // ---------------------------------------------------------------

    public static String resolve(String jsonSnapshot, String expression) {
        return resolve(jsonSnapshot, null, expression);
    }

    public static String resolve(String jsonSnapshot, String rootScope, String expression) {
        if (jsonSnapshot == null || expression == null || expression.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(jsonSnapshot);
            JsonNode start = resolveScope(root, rootScope);
            if (start == null) return null;
            NavResult nav = navigateWithPath(start, expression.trim());
            return extractValue(nav.node);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------
    //  API pública — resolveDetailed (POJO)
    // ---------------------------------------------------------------

    public static ResolutionResult resolveDetailed(String jsonSnapshot, String expression) {
        return resolveDetailed(jsonSnapshot, null, expression);
    }

    public static ResolutionResult resolveDetailed(String jsonSnapshot, String rootScope, String expression) {
        long startNanos = System.nanoTime();

        List<String> tokens = (expression == null || expression.isBlank())
                ? List.of() : tokenize(expression.trim());
        String suggested = computeSuggested(tokens);

        // Validaciones de entrada
        if (jsonSnapshot == null) {
            return failedResult(null, rootScope, expression, suggested,
                    "Snapshot is null", asScope(rootScope), null, startNanos);
        }
        if (expression == null) {
            return failedResult(jsonSnapshot, rootScope, null, suggested,
                    "Expression is null", asScope(rootScope), null, startNanos);
        }
        if (expression.isBlank()) {
            return failedResult(jsonSnapshot, rootScope, expression, suggested,
                    "Expression is " + describeBlank(expression), asScope(rootScope), null, startNanos);
        }

        // Parsear JSON
        JsonNode root;
        try {
            root = MAPPER.readTree(jsonSnapshot);
        } catch (JsonParseException e) {
            return failedResult(jsonSnapshot, rootScope, expression, suggested,
                    "Invalid JSON: " + e.getOriginalMessage(), "root",
                    List.of("Verify JSON syntax near line " + e.getLocation().getLineNr()), startNanos);
        } catch (Exception e) {
            return failedResult(jsonSnapshot, rootScope, expression, suggested,
                    "Invalid JSON: " + e.getMessage(), "root",
                    List.of("Verify the JSON structure"), startNanos);
        }

        // Resolver scope
        JsonNode start = resolveScope(root, rootScope);
        if (start == null) {
            return failedResult(jsonSnapshot, rootScope, expression, suggested,
                    "Scope '" + rootScope + "' not found in root", "root",
                    buildScopeSuggestions(root, rootScope), startNanos);
        }

        // Navegar
        NavResult nav = navigateWithPath(start, tokens);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (nav.node != null) {
            String value = extractValue(nav.node);
            return new ResolutionResult(jsonSnapshot, rootScope, expression, suggested,
                    value, elapsedMs, nav.resolvedPath, null);
        }

        ResolutionError error = buildError(nav, rootScope);
        return new ResolutionResult(jsonSnapshot, rootScope, expression, suggested,
                null, elapsedMs, nav.resolvedPath, error);
    }

    // ---------------------------------------------------------------
    //  API pública — resolveDetailedAsJson (String)
    // ---------------------------------------------------------------

    public static String resolveDetailedAsJson(String jsonSnapshot, String expression) {
        return resolveDetailedAsJson(jsonSnapshot, null, expression);
    }

    public static String resolveDetailedAsJson(String jsonSnapshot, String rootScope, String expression) {
        try {
            ResolutionResult result = resolveDetailed(jsonSnapshot, rootScope, expression);
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            ResolutionResult fallback = failedResult(jsonSnapshot, rootScope, expression, null,
                    "Serialization error: " + e.getMessage(), asScope(rootScope), null, System.nanoTime());
            try {
                return MAPPER.writeValueAsString(fallback);
            } catch (Exception ex) {
                return "{\"error\":{\"message\":\"Critical serialization failure\"}}";
            }
        }
    }

    // ---------------------------------------------------------------
    //  Internos: construcción de resultado detallado
    // ---------------------------------------------------------------

    private static String computeSuggested(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (String seg : tokens) {
            if (!sb.isEmpty()) sb.append('.');
            if (seg.startsWith("[")) {
                String inner = seg.substring(1, seg.length() - 1);
                if (isQuoted(inner)) {
                    sb.append('[').append(inner.substring(1, inner.length() - 1)).append(']');
                    continue;
                }
                if (inner.chars().allMatch(Character::isDigit)
                        || (inner.startsWith("-") && inner.substring(1).chars().allMatch(Character::isDigit))) {
                    sb.append(inner);
                    continue;
                }
            }
            sb.append(seg);
        }
        return sb.toString();
    }

    private static String describeBlank(String s) {
        if (s == null) return "null";
        return s.isEmpty() ? "EMPTY" : "BLANK";
    }

    private static String asScope(String scope) {
        return scope == null ? null : scope.trim();
    }

    private static ResolutionResult failedResult(String snapshot, String scope, String expr,
                                                  String suggested, String message, String lastPath,
                                                  List<String> suggestions, long startNanos) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sug = suggestions != null ? suggestions : List.of();
        ResolutionError err = new ResolutionError(message, lastPath, sug);
        return new ResolutionResult(snapshot, scope, expr, suggested, null,
                elapsed, "", err);
    }

    private static ResolutionError buildError(NavResult nav, String rootScope) {
        String seg = nav.failedSegment;
        JsonNode parent = nav.parentNode;
        String displayPath = nav.resolvedPath.isEmpty()
                ? "snapshot JSON" : nav.resolvedPath;
        String lastPath = nav.resolvedPath.isEmpty()
                ? asScope(rootScope) : nav.resolvedPath;

        String message = "Field '" + seg + "' not found in '" + displayPath + "'";
        List<String> suggestions = buildSuggestions(parent, seg, displayPath);
        return new ResolutionError(message, lastPath, suggestions);
    }

    private static List<String> buildSuggestions(JsonNode parent, String segment, String nodePath) {
        List<String> sug = new ArrayList<>();

        if (parent != null && parent.isObject()) {
            List<String> fields = new ArrayList<>();
            parent.fieldNames().forEachRemaining(fields::add);
            if (!fields.isEmpty()) {
                sug.add("Available fields in '" + nodePath + "': " + String.join(", ", fields));
            } else {
                sug.add("Node '" + nodePath + "' is an object with no fields");
            }
        } else if (parent != null && parent.isArray()) {
            sug.add("Node '" + nodePath + "' is an array with " + parent.size()
                    + " elements. Use array index syntax [n] or [-n]");
        }

        if (segment != null && segment.contains(".") && !segment.startsWith("[")) {
            sug.add("Field '" + segment + "' contains a dot. Try bracket syntax: [\"" + segment + "\"]");
        }

        if (sug.isEmpty()) {
            sug.add("Verify the expression syntax and the JSON structure");
        }

        return sug;
    }

    private static List<String> buildScopeSuggestions(JsonNode root, String scope) {
        List<String> sug = new ArrayList<>();
        if (root != null && root.isObject()) {
            List<String> topFields = new ArrayList<>();
            root.fieldNames().forEachRemaining(topFields::add);
            if (!topFields.isEmpty()) {
                sug.add("Available root fields: " + String.join(", ", topFields));
            }
        }
        sug.add("Verify that the scope path exists in the JSON structure");
        return sug;
    }

    // ---------------------------------------------------------------
    //  Scope
    // ---------------------------------------------------------------

    private static JsonNode resolveScope(JsonNode root, String scope) {
        if (scope == null) return root;
        String s = scope.trim();
        return s.isEmpty() ? root : navigateWithPath(root, s).node;
    }

    // ---------------------------------------------------------------
    //  Navegación con tracking de ruta
    // ---------------------------------------------------------------

    /**
     * Navega desde un nodo siguiendo una ruta de segmentos, pero
     * captura el punto exacto de falla en un {@link NavResult}.
     */
    private static NavResult navigateWithPath(JsonNode start, String path) {
        return navigateWithPath(start, tokenize(path));
    }

    private static NavResult navigateWithPath(JsonNode start, List<String> tokens) {
        JsonNode current = start;
        StringBuilder resolved = new StringBuilder();

        for (String segment : tokens) {
            JsonNode next = navigateSegment(current, segment);
            if (next == null) {
                return new NavResult(null, resolved.toString(), segment, current);
            }
            if (segment.startsWith("[")) {
                resolved.append(segment);
            } else {
                if (!resolved.isEmpty()) resolved.append(".");
                resolved.append(segment);
            }
            current = next;
        }
        return new NavResult(current, resolved.toString(), null, null);
    }

    // ---------------------------------------------------------------
    //  Tokenizer
    // ---------------------------------------------------------------

    private static List<String> tokenize(String path) {
        List<String> raw = new ArrayList<>();
        int start = 0, depth = 0;
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (inQuote) {
                if (c == quoteChar) inQuote = false;
                continue;
            }
            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
                continue;
            }
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == '.' && depth == 0 && !inQuote) {
                if (i > start) raw.add(path.substring(start, i));
                start = i + 1;
            }
        }
        if (start < path.length()) raw.add(path.substring(start));

        List<String> tokens = new ArrayList<>();
        for (String seg : raw) {
            if (seg.isEmpty()) continue;
            int idx = seg.indexOf('[');
            if (idx > 0) {
                tokens.add(seg.substring(0, idx));
                splitBracketGroups(seg.substring(idx), tokens);
            } else if (idx == 0) {
                splitBracketGroups(seg, tokens);
            } else {
                tokens.add(seg);
            }
        }
        return tokens;
    }

    private static void splitBracketGroups(String s, List<String> out) {
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '[') {
                int depth = 0, bStart = i;
                boolean inQ = false;
                char qc = 0;
                while (i < s.length()) {
                    char c = s.charAt(i);
                    if (inQ) {
                        if (c == qc) inQ = false;
                    } else if (c == '"' || c == '\'') {
                        inQ = true;
                        qc = c;
                    } else if (c == '[') {
                        depth++;
                    } else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            out.add(s.substring(bStart, i + 1));
                            i++;
                            break;
                        }
                    }
                    i++;
                }
            } else {
                i++;
            }
        }
    }

    // ---------------------------------------------------------------
    //  Navegación de segmento
    // ---------------------------------------------------------------

    /**
     * Resuelve un segmento contra el nodo actual.
     * El orden de evaluación evita ambigüedad entre arrays y mapas.
     */
    private static JsonNode navigateSegment(JsonNode node, String segment) {
        if (segment.startsWith("[")) {
            return bracketAccess(node, segment);
        }
        if (segment.chars().allMatch(Character::isDigit) || segment.matches("-\\d+")) {
            return arrayIndex(node, Integer.parseInt(segment));
        }
        return node.get(segment);
    }

    // ---------------------------------------------------------------
    //  Acceso bracket: [n], [-n], ["key"], ['key'], [key]
    // ---------------------------------------------------------------

    private static JsonNode bracketAccess(JsonNode node, String segment) {
        String inner = segment.substring(1, segment.length() - 1);
        if (inner.isEmpty()) {
            return null;
        }
        if (isQuoted(inner)) {
            String key = inner.substring(1, inner.length() - 1);
            return node.get(key);
        }
        if (inner.charAt(0) == '-' || inner.chars().allMatch(Character::isDigit)) {
            try {
                return arrayIndex(node, Integer.parseInt(inner));
            } catch (NumberFormatException e) {
                return node.get(inner);
            }
        }
        return node.get(inner);
    }

    private static boolean isQuoted(String s) {
        return (s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'"));
    }

    // ---------------------------------------------------------------
    //  Índice de array (soporta negativos)
    // ---------------------------------------------------------------

    private static JsonNode arrayIndex(JsonNode node, int index) {
        if (!node.isArray()) {
            return null;
        }
        int adjusted = index >= 0 ? index : node.size() + index;
        return adjusted >= 0 && adjusted < node.size() ? node.get(adjusted) : null;
    }

    // ---------------------------------------------------------------
    //  Extracción de valor (nativa, sin _value / _toString)
    // ---------------------------------------------------------------

    /**
     * Extrae el valor textual de un nodo JSON:
     * <ul>
     *   <li>TextNode → {@code asText()}</li>
     *   <li>NumericNode → {@code asText()}</li>
     *   <li>BooleanNode → {@code "true"} / {@code "false"}</li>
     *   <li>ObjectNode con {@code _toString} → valor de esa propiedad</li>
     *   <li>ObjectNode / ArrayNode → {@code node.toString()} (JSON plano)</li>
     *   <li>NullNode → {@code null}</li>
     * </ul>
     */
    private static String extractValue(JsonNode node) {
        if (node == null) return null;
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.asText();
        if (node.isBoolean()) return Boolean.toString(node.asBoolean());
        if (node.isObject()) {
            JsonNode toString = node.get("_toString");
            if (toString != null && toString.isTextual()) return toString.asText();
            return node.toString();
        }
        if (node.isArray()) return node.toString();
        return null;
    }

    // ---------------------------------------------------------------
    //  Inner class: resultado intermedio de navegación
    // ---------------------------------------------------------------

    private static class NavResult {
        final JsonNode node;
        final String resolvedPath;
        final String failedSegment;
        final JsonNode parentNode;

        NavResult(JsonNode node, String resolvedPath, String failedSegment, JsonNode parentNode) {
            this.node = node;
            this.resolvedPath = resolvedPath;
            this.failedSegment = failedSegment;
            this.parentNode = parentNode;
        }
    }
}
