package Filter;

import Util.LogUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controle de acesso da aplicação inteira (P0-1 da auditoria de 29/08/2026).
 *
 * <h3>O que estava errado</h3>
 * <p>O filtro anterior declarava {@code urlPatterns = {"/view/admin/*"}}, ou
 * seja, protegia as <b>páginas</b>. Só que os 21 servlets estão mapeados na
 * raiz do contexto — {@code /ControllerCLT}, {@code /ControllerFuncionario},
 * {@code /RelatorioExportServlet} — e portanto ficavam de fora. Como nenhum
 * servlet checava a sessão para autorizar, a API inteira respondia a qualquer
 * um. Bastava saber a URL para listar funcionários, alterar a folha ou baixar
 * a planilha de pagamento.</p>
 *
 * <h3>Como ficou</h3>
 * <p>O filtro agora cobre {@code /*} e trabalha por <b>negação padrão</b>: tudo
 * exige sessão, exceto o que está explicitamente na lista pública (tela de
 * login, o próprio {@code LoginServlet} e os arquivos estáticos). Assim, um
 * servlet novo nasce protegido — o erro que gerou este achado não pode se
 * repetir por esquecimento.</p>
 *
 * <h3>Resposta adequada ao tipo de chamada</h3>
 * <p>Navegação de página sem sessão continua sendo redirecionada ao login,
 * preservando o destino. Chamada AJAX, porém, recebe <b>401 com JSON</b>: antes,
 * o redirecionamento fazia o JavaScript receber o HTML da tela de login onde
 * esperava dados, e o erro aparecia como uma falha de parse sem sentido.</p>
 */
@WebFilter(urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    /** Caminhos exatos liberados sem sessão. */
    private static final Set<String> PUBLICOS_EXATOS = new HashSet<>(Arrays.asList(
        // "/" NÃO entra aqui: a raiz cai no welcome-file index.jsp e serviria o
        // painel (estrutura, menu, títulos) a quem não fez login. Sem sessão, a
        // raiz agora redireciona para a tela de login, como deve ser.
        "/login.jsp", "/LoginServlet", "/LogoutServlet", "/favicon.ico"
    ));

    /** Prefixos liberados: recursos estáticos servidos ao navegador. */
    private static final List<String> PUBLICOS_PREFIXO = Arrays.asList(
        "/CSS/", "/JS/", "/img/", "/images/", "/webjars/", "/pagina/importacao.html"
    );

    /** Extensões estáticas liberadas em qualquer pasta. */
    private static final List<String> EXTENSOES_ESTATICAS = Arrays.asList(
        ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".woff", ".woff2", ".ttf", ".map"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LogUtil.info(AuthFilter.class,
                "Controle de acesso ativo em /* — acesso negado por padrão.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // P2-21: encoding definido em um só lugar. Antes, ControllerPontoEletronico
        // e RelatorioExportServlet não chamavam setCharacterEncoding e gravavam
        // acentuação corrompida ("AÃ§Ã£o") no banco.
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }

        String caminho = req.getRequestURI().substring(req.getContextPath().length());
        if (caminho.isEmpty()) caminho = "/";

        if (ehPublico(caminho)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession sessao = req.getSession(false);
        boolean autenticado = sessao != null && sessao.getAttribute("usuarioLogado") != null;

        if (autenticado) {
            // Páginas autenticadas não podem ficar no cache do navegador: sem isto,
            // o botão "voltar" depois do logout ainda mostra os dados.
            if (!ehEstatico(caminho)) {
                res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                res.setHeader("Pragma", "no-cache");
            }
            chain.doFilter(request, response);
            return;
        }

        negar(req, res, caminho);
    }

    private boolean ehPublico(String caminho) {
        if (PUBLICOS_EXATOS.contains(caminho)) return true;
        for (String p : PUBLICOS_PREFIXO) {
            if (caminho.startsWith(p)) return true;
        }
        return ehEstatico(caminho);
    }

    private boolean ehEstatico(String caminho) {
        String minusculo = caminho.toLowerCase();
        for (String ext : EXTENSOES_ESTATICAS) {
            if (minusculo.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Distingue chamada de dados de navegação de página. Um XMLHttpRequest ou
     * um fetch que pede JSON precisa de um 401 legível; um clique em link
     * precisa da tela de login.
     */
    private boolean ehChamadaDeDados(HttpServletRequest req) {
        String comXhr   = req.getHeader("X-Requested-With");
        String aceita   = req.getHeader("Accept");
        String tipo     = req.getContentType();
        return "XMLHttpRequest".equalsIgnoreCase(comXhr)
            || (aceita != null && aceita.contains("application/json"))
            || (tipo   != null && tipo.contains("application/json"));
    }

    private void negar(HttpServletRequest req, HttpServletResponse res, String caminho)
            throws IOException {

        LogUtil.aviso(AuthFilter.class,
                "Acesso sem sessão bloqueado em " + req.getMethod() + " " + caminho, null);

        if (ehChamadaDeDados(req)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = res.getWriter()) {
                out.print("{\"ok\":false,\"autenticado\":false,"
                        + "\"msg\":\"Sua sessão expirou. Faça login novamente.\"}");
            }
            return;
        }

        String query   = req.getQueryString();
        String destino = req.getRequestURI() + (query != null ? "?" + query : "");
        res.sendRedirect(req.getContextPath() + "/login.jsp?redirect="
                + URLEncoder.encode(destino, StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() { }
}
