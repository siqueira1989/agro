package Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitário de hash para auditoria/imutabilidade (Portaria 671 do MTE).
 * Gera o SHA-256 hexadecimal de uma string canônica do registro de ponto,
 * permitindo detectar adulteração posterior dos dados.
 */
public final class HashUtil {

    private HashUtil() { }

    public static String sha256(String input) {
        if (input == null) input = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido pela plataforma; não deve ocorrer.
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
