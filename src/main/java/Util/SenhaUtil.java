package Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senha com PBKDF2 (P0-2 da auditoria de 29/08/2026).
 *
 * <p><b>O problema.</b> A coluna {@code pessoa.senhapessoa} guardava a senha em
 * texto puro e o login a comparava literalmente no {@code WHERE}. Um vazamento
 * do dump — ou a própria API aberta — entregava todas as credenciais.</p>
 *
 * <p><b>A escolha.</b> {@code PBKDF2WithHmacSHA256} vem no próprio JDK
 * ({@code javax.crypto}), então atende à restrição de não adicionar framework
 * nem biblioteca ao backend. É um algoritmo de derivação lento e com salt, ou
 * seja: duas pessoas com a mesma senha geram hashes diferentes, e testar senhas
 * por força bruta fica caro. O número de iterações é configurável em
 * {@code seguranca.pbkdf2.iteracoes} para poder ser elevado no futuro sem
 * invalidar os hashes já gravados — ele viaja dentro do próprio hash.</p>
 *
 * <h3>Formato gravado</h3>
 * <pre>pbkdf2$sha256$&lt;iteracoes&gt;$&lt;salt base64&gt;$&lt;hash base64&gt;</pre>
 * <p>Cabe em {@code varchar(255)} — a migration V20 aumenta a coluna, que era
 * {@code varchar(50)}.</p>
 */
public final class SenhaUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String MARCADOR  = "pbkdf2$sha256$";
    private static final int TAMANHO_SALT_BYTES = 16;
    private static final int TAMANHO_CHAVE_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaUtil() { }

    private static int iteracoes() {
        return Math.max(10000, ConfigUtil.getInt("seguranca.pbkdf2.iteracoes", 210000));
    }

    /** Gera o hash de uma senha em texto puro, com salt novo e aleatório. */
    public static String gerarHash(String senhaPlana) {
        if (senhaPlana == null) senhaPlana = "";
        byte[] salt = new byte[TAMANHO_SALT_BYTES];
        RANDOM.nextBytes(salt);
        int iter = iteracoes();
        byte[] hash = derivar(senhaPlana, salt, iter);
        return MARCADOR + iter + "$"
             + Base64.getEncoder().encodeToString(salt) + "$"
             + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Confere a senha digitada contra o valor gravado.
     *
     * <p>Se o valor gravado ainda estiver em texto puro (base não migrada),
     * a comparação é feita em tempo constante e um aviso vai para o log — o
     * login não quebra durante a transição, mas o fato fica registrado.</p>
     */
    public static boolean verificar(String senhaPlana, String armazenado) {
        if (senhaPlana == null || armazenado == null || armazenado.isBlank()) return false;

        if (!estaEmHash(armazenado)) {
            LogUtil.aviso(SenhaUtil.class,
                    "Senha ainda em texto puro no banco. Rode a migração de senhas "
                  + "(seguranca.migrarSenhasNoBoot=true).", null);
            return iguaisEmTempoConstante(
                    senhaPlana.getBytes(StandardCharsets.UTF_8),
                    armazenado.getBytes(StandardCharsets.UTF_8));
        }

        try {
            String[] partes = armazenado.split("\\$");
            // pbkdf2 | sha256 | iteracoes | salt | hash
            if (partes.length != 5) return false;
            int iter    = Integer.parseInt(partes[2]);
            byte[] salt = Base64.getDecoder().decode(partes[3]);
            byte[] alvo = Base64.getDecoder().decode(partes[4]);
            byte[] calc = derivar(senhaPlana, salt, iter);
            return iguaisEmTempoConstante(calc, alvo);
        } catch (RuntimeException e) {
            LogUtil.aviso(SenhaUtil.class, "Hash de senha em formato inválido.", e);
            return false;
        }
    }

    /**
     * Prepara o valor que vai para o banco em cadastro e alteração.
     *
     * <p>Devolve string vazia quando nenhuma senha foi digitada — o UPDATE usa
     * {@code COALESCE(NULLIF(?,''), senhapessoa)}, ou seja, campo em branco
     * significa "manter a senha atual". Foi isso que permitiu remover o
     * {@code value="<%= getSenhaPessoa() %>"} do formulário de parceiro, que
     * expunha a senha no HTML só para não perdê-la ao salvar.</p>
     *
     * <p>Se o valor já vier em formato PBKDF2, é repassado sem novo hash, para
     * não gerar hash de hash quando um objeto lido do banco é regravado.</p>
     */
    public static String hashOuVazio(String senhaPlana) {
        if (senhaPlana == null || senhaPlana.isBlank()) return "";
        if (estaEmHash(senhaPlana)) return senhaPlana;
        return gerarHash(senhaPlana);
    }

    /** Indica se o valor já está no formato PBKDF2 (usado pela migração). */
    public static boolean estaEmHash(String valor) {
        return valor != null && valor.startsWith(MARCADOR);
    }

    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        try {
            KeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_CHAVE_BITS);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar o hash da senha.", e);
        }
    }

    /**
     * Comparação em tempo constante: não vaza, pelo tempo de resposta, quantos
     * bytes iniciais estavam corretos.
     */
    private static boolean iguaisEmTempoConstante(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
