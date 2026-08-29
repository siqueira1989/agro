package Util;

/**
 * Escape de HTML para uso nos JSPs (P2-22 da auditoria de 29/08/2026).
 *
 * <p><b>O problema.</b> {@code AtualizarParceiro.jsp} imprimia dados do banco
 * com {@code <%= %>} cru dentro de atributos. Um parceiro cadastrado com o nome
 * {@code " onfocus=alert(1) autofocus x="} fechava o atributo e executava
 * script no navegador do próximo usuário que abrisse a tela — XSS armazenado.</p>
 *
 * <p><b>Por que não JSTL.</b> {@code <c:out>} e {@code fn:escapeXml} exigem a
 * dependência {@code jakarta.servlet.jsp.jstl}, que não está no
 * {@code pom.xml}. Esta classe resolve com o JDK apenas, respeitando a
 * restrição de não adicionar biblioteca ao backend.</p>
 *
 * <p>Escapa os cinco caracteres que importam. As aspas simples e duplas são
 * essenciais: sem elas, o escape protege o corpo do HTML mas não o interior de
 * um atributo, que é exatamente onde estava a falha.</p>
 */
public final class HtmlUtil {

    private HtmlUtil() { }

    /** Texto seguro para o corpo do HTML e para dentro de atributos. */
    public static String esc(String valor) {
        if (valor == null) return "";
        StringBuilder sb = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#39;");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Versão para números e outros objetos; {@code null} vira string vazia. */
    public static String esc(Object valor) {
        return valor == null ? "" : esc(String.valueOf(valor));
    }
}
