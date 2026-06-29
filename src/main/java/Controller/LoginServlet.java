package Controller;

import Model.Dao.PessoaDAO;
import Model.Model.Pessoa;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private PessoaDAO pessoaDAO;

    @Override
    public void init() {
        pessoaDAO = new PessoaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            resp.sendRedirect(req.getContextPath() + "/view/admin/index.jsp");
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) json.append(linha);
        }

        JsonObject jo = gson.fromJson(json.toString(), JsonObject.class);
        String usuario = jo.has("usuario") && !jo.get("usuario").isJsonNull()
                ? jo.get("usuario").getAsString() : null;
        String senha = jo.has("senha") && !jo.get("senha").isJsonNull()
                ? jo.get("senha").getAsString() : null;

        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Usuário e senha são obrigatórios.");
            return;
        }

        try {
            Pessoa credenciais = new Pessoa();
            credenciais.setUsuarioPessoa(usuario.trim());
            credenciais.setSenhaPessoa(senha);

            Pessoa pessoaLogada = pessoaDAO.validarUsuario(credenciais);

            if (pessoaLogada == null) {
                writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED, false, "Usuário ou senha inválidos.");
                return;
            }
            if (!pessoaLogada.isSituacaoPessoa()) {
                writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED, false, "Usuário inativo. Contate o administrador.");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("usuarioLogado", pessoaLogada);
            session.setMaxInactiveInterval(30 * 60);

            writeJson(resp, HttpServletResponse.SC_OK, true, "Login realizado com sucesso.");

        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao realizar login. Tente novamente.");
        }
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", ok);
        payload.put("msg", msg);
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }
}
