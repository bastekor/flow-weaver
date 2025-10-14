package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String NUMERIC = "0123456789";
    private static final String ALPHA_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String HEX = "0123456789ABCDEF";
    private static final Random RANDOM = new SecureRandom();

    // ============================================================
    // OPCIÓN 1: Alfanumérico (A-Z, 0-9)
    // Ejemplo: A3F9-K2M7
    // ============================================================

    /**
     * Genera código alfanumérico: XXXX-XXXX
     * @return String de 9 caracteres (4 + guion + 4)
     */
    public static String generateAlphanumeric() {
        StringBuilder code = new StringBuilder(9);

        // Primera parte (4 caracteres)
        for (int i = 0; i < 4; i++) {
            code.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }

        code.append('-');

        // Segunda parte (4 caracteres)
        for (int i = 0; i < 4; i++) {
            code.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 2: Solo números (0-9)
    // Ejemplo: 1234-5678
    // ============================================================

    /**
     * Genera código numérico: XXXX-XXXX
     * @return String de 9 caracteres solo números
     */
    public static String generateNumeric() {
        StringBuilder code = new StringBuilder(9);

        for (int i = 0; i < 4; i++) {
            code.append(NUMERIC.charAt(RANDOM.nextInt(NUMERIC.length())));
        }

        code.append('-');

        for (int i = 0; i < 4; i++) {
            code.append(NUMERIC.charAt(RANDOM.nextInt(NUMERIC.length())));
        }

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 3: Solo letras mayúsculas (A-Z)
    // Ejemplo: ABCD-EFGH
    // ============================================================

    /**
     * Genera código solo letras: XXXX-XXXX
     * @return String de 9 caracteres solo letras
     */
    public static String generateAlphaOnly() {
        StringBuilder code = new StringBuilder(9);

        for (int i = 0; i < 4; i++) {
            code.append(ALPHA_UPPER.charAt(RANDOM.nextInt(ALPHA_UPPER.length())));
        }

        code.append('-');

        for (int i = 0; i < 4; i++) {
            code.append(ALPHA_UPPER.charAt(RANDOM.nextInt(ALPHA_UPPER.length())));
        }

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 4: Hexadecimal (0-9, A-F)
    // Ejemplo: A3F9-2B7C
    // ============================================================

    /**
     * Genera código hexadecimal: XXXX-XXXX
     * @return String de 9 caracteres hexadecimal
     */
    public static String generateHex() {
        StringBuilder code = new StringBuilder(9);

        for (int i = 0; i < 4; i++) {
            code.append(HEX.charAt(RANDOM.nextInt(HEX.length())));
        }

        code.append('-');

        for (int i = 0; i < 4; i++) {
            code.append(HEX.charAt(RANDOM.nextInt(HEX.length())));
        }

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 5: Desde UUID (primeros 9 caracteres con guion)
    // Ejemplo: 3A2F-1B9E
    // ============================================================

    /**
     * Genera código desde UUID: XXXX-XXXX
     * @return String de 9 caracteres desde UUID
     */
    public static String generateFromUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
    }

    // ============================================================
    // OPCIÓN 6: Con timestamp (único por milisegundo)
    // Ejemplo: A3F9-1234 (últimos 4 dígitos del timestamp)
    // ============================================================

    /**
     * Genera código con timestamp para garantizar unicidad: XXXX-XXXX
     * Primera parte: random
     * Segunda parte: timestamp (últimos 4 caracteres)
     * @return String de 9 caracteres semi-único
     */
    public static String generateWithTimestamp() {
        StringBuilder code = new StringBuilder(9);

        // Primera parte random
        for (int i = 0; i < 4; i++) {
            code.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }

        code.append('-');

        // Segunda parte con timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String last4 = timestamp.substring(timestamp.length() - 4);
        code.append(last4);

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 7: Personalizable (charset custom)
    // ============================================================

    /**
     * Genera código con charset personalizado: XXXX-XXXX
     * @param charset Caracteres permitidos
     * @return String de 9 caracteres
     */
    public static String generateCustom(String charset) {
        if (charset == null || charset.isEmpty()) {
            throw new IllegalArgumentException("Charset no puede ser vacío");
        }

        StringBuilder code = new StringBuilder(9);

        for (int i = 0; i < 4; i++) {
            code.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }

        code.append('-');

        for (int i = 0; i < 4; i++) {
            code.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }

        return code.toString();
    }

    // ============================================================
    // OPCIÓN 8: Compacto (usando Math.random y StringBuilder)
    // ============================================================

    /**
     * Genera código alfanumérico compacto con prefijo: <<prefijo>>XXXX-XXXX
     * Versión más corta y simple
     * @param prefix Prefijo a incluir
     * @return String de 9 caracteres
     */
    public static String generate(final String prefix) {
        return prefix + "#" + generate();
    }

    /**
     * Genera código alfanumérico compacto: XXXX-XXXX
     * Versión más corta y simple
     * @return String de 9 caracteres
     */
    public static String generate() {
        return String.format("%s-%s",
                randomString(4),
                randomString(4));
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}