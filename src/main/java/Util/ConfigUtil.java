package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuração externa da aplicação (P0-3 da auditoria de 29/08/2026).
 *
 * <p>Nenhuma credencial fica no código-fonte. Os valores são procurados nesta
 * ordem, e o primeiro arquivo encontrado vence:</p>
 *
 * <ol>
 *   <li>caminho indicado em {@code -Dagro.config=/etc/agro/agro.properties};</li>
 *   <li>variável de ambiente {@code AGRO_CONFIG};</li>
 *   <li>{@code $CATALINA_BASE/conf/agro.properties} (fora do WAR — recomendado);</li>
 *   <li>{@code agro.properties} no classpath (útil só em desenvolvimento).</li>
 * </ol>
 *
 * <p>Qualquer chave pode ainda ser sobrescrita individualmente por uma
 * propriedade de sistema de mesmo nome ({@code -Ddb.password=...}), o que
 * permite injetar segredos sem tocar em arquivo.</p>
 *
 * <p>Segue o mesmo padrão que {@code EmailUtil} já usava para o SMTP; a
 * auditoria apenas o estendeu ao banco de dados.</p>
 */
public final class ConfigUtil {

    private static final String ARQUIVO = "agro.properties";
    private static final Properties PROPS = new Properties();
    private static volatile String origem = "valores padrão (nenhum arquivo encontrado)";

    static {
        carregar();
    }

    private ConfigUtil() { }

    private static void carregar() {
        File[] candidatos = {
            caminho(System.getProperty("agro.config")),
            caminho(System.getenv("AGRO_CONFIG")),
            caminho(concat(System.getProperty("catalina.base"), "conf", ARQUIVO)),
            caminho(concat(System.getenv("CATALINA_BASE"), "conf", ARQUIVO))
        };

        for (File f : candidatos) {
            if (f != null && f.isFile()) {
                try (InputStream in = new FileInputStream(f)) {
                    PROPS.load(in);
                    origem = f.getAbsolutePath();
                    return;
                } catch (Exception e) {
                    LogUtil.aviso(ConfigUtil.class,
                            "Falha ao ler " + f.getAbsolutePath() + "; tentando a próxima origem.", e);
                }
            }
        }

        try (InputStream in = ConfigUtil.class.getClassLoader().getResourceAsStream(ARQUIVO)) {
            if (in != null) {
                PROPS.load(in);
                origem = "classpath:" + ARQUIVO;
                return;
            }
        } catch (Exception e) {
            LogUtil.aviso(ConfigUtil.class, "Falha ao ler " + ARQUIVO + " do classpath.", e);
        }

        LogUtil.aviso(ConfigUtil.class,
                "Nenhum " + ARQUIVO + " encontrado. Configure -Dagro.config, AGRO_CONFIG "
              + "ou $CATALINA_BASE/conf/" + ARQUIVO + ".", null);
    }

    private static File caminho(String p) {
        return (p == null || p.isBlank()) ? null : new File(p);
    }

    private static String concat(String base, String... partes) {
        if (base == null || base.isBlank()) return null;
        StringBuilder sb = new StringBuilder(base);
        for (String p : partes) sb.append(File.separator).append(p);
        return sb.toString();
    }

    /** Onde a configuração foi lida — usado no log de inicialização. */
    public static String origem() {
        return origem;
    }

    /** Valor de texto; a propriedade de sistema de mesmo nome tem prioridade. */
    public static String get(String chave, String padrao) {
        String sys = System.getProperty(chave);
        if (sys != null && !sys.isBlank()) return sys;
        String v = PROPS.getProperty(chave);
        return (v == null || v.isBlank()) ? padrao : v.trim();
    }

    /** Valor obrigatório: falha cedo e com mensagem clara se estiver ausente. */
    public static String obrigatorio(String chave) {
        String v = get(chave, null);
        if (v == null) {
            throw new IllegalStateException(
                "Configuração obrigatória ausente: '" + chave + "'. "
              + "Defina-a em " + ARQUIVO + " (origem atual: " + origem + ") "
              + "ou passe -D" + chave + "=... na inicialização.");
        }
        return v;
    }

    public static int getInt(String chave, int padrao) {
        try {
            return Integer.parseInt(get(chave, String.valueOf(padrao)));
        } catch (NumberFormatException e) {
            LogUtil.aviso(ConfigUtil.class,
                    "Valor inválido para '" + chave + "'; usando o padrão " + padrao + ".", null);
            return padrao;
        }
    }

    public static boolean getBoolean(String chave, boolean padrao) {
        return Boolean.parseBoolean(get(chave, String.valueOf(padrao)));
    }
}
