package Util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log da aplicação (P2-20 da auditoria de 29/08/2026).
 *
 * <p>Substitui os 38 {@code printStackTrace()} espalhados pelo projeto. Usa
 * {@code java.util.logging}, que já vem no JDK — nenhuma biblioteca nova é
 * adicionada ao {@code pom.xml}, conforme a restrição de não usar framework
 * no backend.</p>
 *
 * <h3>Código de ocorrência</h3>
 * <p>{@link #erro(Class, String, Throwable)} devolve um código curto (ex.:
 * {@code AGRO-7K3F9Q}) que vai para o log junto com a pilha completa e é o
 * único detalhe entregue ao usuário. Assim o suporte localiza a falha exata
 * no log a partir do que apareceu na tela, sem que a mensagem do PostgreSQL —
 * nome de tabela, coluna, constraint — vaze para o navegador (P1-15).</p>
 */
public final class LogUtil {

    private static final String PREFIXO = "AGRO-";
    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private LogUtil() { }

    private static Logger logger(Class<?> origem) {
        return Logger.getLogger(origem.getName());
    }

    /**
     * Registra uma falha com a pilha completa e devolve o código de ocorrência
     * que deve ser mostrado ao usuário.
     */
    public static String erro(Class<?> origem, String contexto, Throwable t) {
        String codigo = novoCodigo();
        logger(origem).log(Level.SEVERE, codigo + " " + contexto, t);
        return codigo;
    }

    public static void aviso(Class<?> origem, String mensagem, Throwable t) {
        if (t != null) logger(origem).log(Level.WARNING, mensagem, t);
        else logger(origem).log(Level.WARNING, mensagem);
    }

    public static void info(Class<?> origem, String mensagem) {
        logger(origem).log(Level.INFO, mensagem);
    }

    public static void debug(Class<?> origem, String mensagem) {
        logger(origem).log(Level.FINE, mensagem);
    }

    /**
     * Mensagem segura para o usuário final: diz o que falhou e traz o código
     * para o suporte, sem nenhum detalhe técnico do banco.
     */
    public static String mensagemUsuario(String acao, String codigo) {
        return "Não foi possível " + acao + ". Tente novamente; se persistir, "
             + "informe o código " + codigo + " ao suporte.";
    }

    private static String novoCodigo() {
        StringBuilder sb = new StringBuilder(PREFIXO);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) sb.append(ALFABETO[r.nextInt(ALFABETO.length)]);
        return sb.toString();
    }
}
