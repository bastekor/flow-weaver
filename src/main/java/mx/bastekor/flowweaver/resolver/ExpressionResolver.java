package mx.bastekor.flowweaver.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *   args[n]  arg[n]    → array index con prefijo args/arg + brackets
 *   argsn   argn       → array index con prefijo args/arg desnudo
 *   args_n  arg_n      → array index con guión bajo
 *   args-n  arg-n      → array index con guión medio
 * </pre>
 * <p>
 * Los segmentos se separan por punto ({@code .}). Un segmento puede
 * contener {@code campo[índice]} y se expande automáticamente a dos
 * pasos de navegación. No existe auto-unwrap ni lógica de terceros.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExpressionResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern ARGS_PATTERN =
            Pattern.compile("^(args|arg)(?:\\[(-?\\d+)]|_(-?\\d+)|(-?\\d+))$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern BRACKET_GROUP =
            Pattern.compile("\\[[^]]*]"); // fallback no usado

    // ---------------------------------------------------------------
    //  API pública
    // ---------------------------------------------------------------

    /**
     * Resuelve una expresión desde la raíz del JSON.
     *
     * @param jsonSnapshot JSON string
     * @param expression   expresión de acceso
     * @return valor textual del nodo terminal, o {@code null}
     */
    public static String resolve(String jsonSnapshot, String expression) {
        return resolve(jsonSnapshot, "json", expression);
    }

    /**
     * Resuelve una expresión contra un nodo específico del JSON.
     * <p>
     * El {@code rootScope} indica el punto de partida. Ejemplos:
     * <ul>
     *   <li>{@code "json"} → la raíz</li>
     *   <li>{@code "_args"} → el array de argumentos</li>
     *   <li>{@code "json._args"} → raíz + _args</li>
     *   <li>{@code "data.users"} → cualquier ruta</li>
     * </ul>
     *
     * @param jsonSnapshot JSON string
     * @param rootScope    nodo de partida ({@code null} → {@code "json"})
     * @param expression   expresión relativa al {@code rootScope}
     * @return valor textual del nodo terminal, o {@code null}
     */
    public static String resolve(String jsonSnapshot, String rootScope, String expression) {
        if (jsonSnapshot == null || expression == null || expression.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(jsonSnapshot);
            JsonNode start = resolveScope(root, rootScope);
            if (start == null) {
                return null;
            }
            JsonNode node = navigate(start, expression.trim());
            return extractValue(node);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------
    //  Scope
    // ---------------------------------------------------------------

    /**
     * Determina el nodo de partida a partir del scope.
     * {@code null} / vacío / {@code "json"} → raíz.
     * {@code "json.algo"} → quita prefijo y navega.
     */
    private static JsonNode resolveScope(JsonNode root, String scope) {
        if (scope == null) {
            return root;
        }
        String s = scope.trim();
        if (s.isEmpty() || "json".equalsIgnoreCase(s)) {
            return root;
        }
        if (s.toLowerCase().startsWith("json.")) {
            s = s.substring(5);
        }
        return s.isEmpty() ? root : navigate(root, s);
    }

    // ---------------------------------------------------------------
    //  Navegación
    // ---------------------------------------------------------------

    /**
     * Navega desde un nodo siguiendo una ruta de segmentos separados por
     * punto. Expande automáticamente {@code campo[índice]} en dos pasos.
     */
    private static JsonNode navigate(JsonNode start, String path) {
        List<String> segments = tokenize(path);
        JsonNode current = start;
        for (String segment : segments) {
            current = navigateSegment(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Convierte una ruta punteada en una lista plana de segmentos.
     * <p>
     * Respeta dots dentro de brackets ({@code config["my.key"]} no se parte
     * por el punto dentro de las comillas). Expande
     * {@code campo[índice]} en {@code campo} + {@code [índice]}.
     */
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

        // Expandir campo[índice] → campo + [índice]
        // Los prefijos args/arg se mantienen unidos para que ARGS_PATTERN
        // pueda capturarlos como una sola unidad sintáctica.
        List<String> tokens = new ArrayList<>();
        for (String seg : raw) {
            if (seg.isEmpty()) continue;
            int idx = seg.indexOf('[');
            if (idx > 0) {
                String prefix = seg.substring(0, idx);
                if (prefix.equalsIgnoreCase("args") || prefix.equalsIgnoreCase("arg")) {
                    tokens.add(seg);
                    continue;
                }
                tokens.add(prefix);
                splitBracketGroups(seg.substring(idx), tokens);
            } else if (idx == 0) {
                splitBracketGroups(seg, tokens);
            } else {
                tokens.add(seg);
            }
        }
        return tokens;
    }

    /**
     * Divide {@code s} en grupos de brackets respetando profundidad
     * (soporta {@code ]} dentro de contenido, ej. {@code ["data[0]"]}).
     */
    private static void splitBracketGroups(String s, List<String> out) {
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '[') {
                int depth = 0, start = i;
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
                            out.add(s.substring(start, i + 1));
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
        Matcher m = ARGS_PATTERN.matcher(segment);
        if (m.find()) {
            String num = m.group(2) != null ? m.group(2)
                    : m.group(3) != null ? m.group(3)
                      : m.group(4);
            return arrayIndex(node, Integer.parseInt(num));
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
     *   <li>ObjectNode, ArrayNode, NullNode → {@code null}</li>
     * </ul>
     */
    private static String extractValue(JsonNode node) {
        if (node == null) return null;
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.asText();
        if (node.isBoolean()) return Boolean.toString(node.asBoolean());
        return null;
    }
}
