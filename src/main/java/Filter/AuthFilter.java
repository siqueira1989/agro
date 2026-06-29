package Filter;

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

@WebFilter(urlPatterns = {"/view/admin/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        HttpSession session = httpReq.getSession(false);
        boolean logado = session != null && session.getAttribute("usuarioLogado") != null;

        if (logado) {
            chain.doFilter(request, response);
        } else {
            String contextPath = httpReq.getContextPath();
            String requestedURI  = httpReq.getRequestURI();
            String queryString   = httpReq.getQueryString();
            String fullURL       = requestedURI + (queryString != null ? "?" + queryString : "");
            String encoded       = java.net.URLEncoder.encode(fullURL,
                                       java.nio.charset.StandardCharsets.UTF_8);
            httpResp.sendRedirect(contextPath + "/login.jsp?redirect=" + encoded);
        }
    }

    @Override
    public void destroy() {}
}
