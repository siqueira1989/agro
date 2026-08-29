package Controller;

import Model.Dao.FolhaPagamentoDAO;
import Model.Dao.FuncionarioCLTDAO;
import Model.Dao.FuncionarioDAO;
import Model.Dao.PontoEletronicoDAO;
import Model.Model.FolhaPagamento;
import Model.Model.Funcionario;
import Model.Model.FuncionarioCLT;
import Model.Model.FuncionarioDiarista;
import Model.Model.FuncionarioEmpreita;
import Model.Model.FuncionarioProducao;
import Model.Model.TipoFuncionario;
import Util.PostgresConnection;
import Util.TransacaoUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControleColaborador extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioDAO funcionarioDao;
    private FuncionarioCLTDAO cltDAO;
    private PontoEletronicoDAO pontoDAO;
    private FolhaPagamentoDAO folhaDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        funcionarioDao = new FuncionarioDAO();
        cltDAO = new FuncionarioCLTDAO();
        pontoDAO = new PontoEletronicoDAO();
        folhaDAO = new FolhaPagamentoDAO();
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", ok);
        payload.put("msg", msg);
        if (target != null) payload.put("target", target);
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            String periodo = req.getParameter("periodo");
            String idFuncionario = req.getParameter("idfuncionario");

            if (periodo != null && !periodo.isBlank()) {
                List<FolhaPagamento> lista = idFuncionario != null
                        ? folhaDAO.listByFuncionario(Integer.parseInt(idFuncionario))
                        : folhaDAO.listByPeriodo(periodo);
                out.print(gson.toJson(lista));
            } else {
                List<Funcionario> lista = funcionarioDao.listAllFuncionario();
                out.print(gson.toJson(lista));
            }
            out.flush();
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControleColaborador.class, "Falha tratada em ControleColaborador.", e);
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro: " + cod, "page");
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
        if (jo == null || !jo.has("acao")) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Ação não informada.", "page");
            return;
        }
        switch (jo.get("acao").getAsString().toLowerCase()) {
            case "gerarfolha": handleGerarFolha(jo, resp); break;
            case "listarfolha": handleListarFolha(jo, resp); break;
            default: writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Ação inválida.", "page");
        }
    }

    private void handleGerarFolha(JsonObject jo, HttpServletResponse resp) throws IOException {
        String periodo = getStr(jo, "periodo");
        if (isBlank(periodo)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Período (YYYY-MM) é obrigatório.", "modal");
            return;
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo);
        } catch (Exception e) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Formato de período inválido. Use YYYY-MM.", "modal");
            return;
        }

        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        try {
            List<FolhaPagamento> lancamentos = new ArrayList<>();

            // CLT: salário mensal fixo
            for (FuncionarioCLT f : cltDAO.listTodos()) {
                BigDecimal valor = f.getSalarioMensal() != null ? f.getSalarioMensal() : BigDecimal.ZERO;
                FolhaPagamento fp = new FolhaPagamento();
                fp.setIdFuncionario(f.getIdPessoa());
                fp.setNomeFuncionario(f.getNomePessoa());
                fp.setPeriodo(periodo);
                fp.setTipoFuncionario(TipoFuncionario.CLT);
                fp.setValorCalculado(valor);
                lancamentos.add(fp);
            }

            // Diarista: dias presentes × valor por dia
            for (Funcionario f : funcionarioDao.listarPorTipoFuncionario(TipoFuncionario.DIARISTA)) {
                int diasPresentes = pontoDAO.contarDiasPresentes(f.getIdPessoa(), inicio, fim);
                // Busca o valorPorDia diretamente do banco via query estendida
                BigDecimal valorDia = buscarValorDiarista(f.getIdPessoa());
                BigDecimal valor = valorDia.multiply(BigDecimal.valueOf(diasPresentes));
                FolhaPagamento fp = new FolhaPagamento();
                fp.setIdFuncionario(f.getIdPessoa());
                fp.setNomeFuncionario(f.getNomePessoa());
                fp.setPeriodo(periodo);
                fp.setTipoFuncionario(TipoFuncionario.DIARISTA);
                fp.setValorCalculado(valor);
                lancamentos.add(fp);
            }

            // Empreita: valor fixo acordado
            for (Funcionario f : funcionarioDao.listarPorTipoFuncionario(TipoFuncionario.EMPREITA)) {
                BigDecimal valor = buscarValorEmpreita(f.getIdPessoa());
                FolhaPagamento fp = new FolhaPagamento();
                fp.setIdFuncionario(f.getIdPessoa());
                fp.setNomeFuncionario(f.getNomePessoa());
                fp.setPeriodo(periodo);
                fp.setTipoFuncionario(TipoFuncionario.EMPREITA);
                fp.setValorCalculado(valor);
                lancamentos.add(fp);
            }

            // Producao: quantidade produzida × valor por unidade
            for (Funcionario f : funcionarioDao.listarPorTipoFuncionario(TipoFuncionario.PRODUCAO)) {
                BigDecimal[] dados = buscarDadosProducao(f.getIdPessoa(), periodo);
                BigDecimal valor = dados[0].multiply(dados[1]);
                FolhaPagamento fp = new FolhaPagamento();
                fp.setIdFuncionario(f.getIdPessoa());
                fp.setNomeFuncionario(f.getNomePessoa());
                fp.setPeriodo(periodo);
                fp.setTipoFuncionario(TipoFuncionario.PRODUCAO);
                fp.setValorCalculado(valor);
                lancamentos.add(fp);
            }

            // Persiste tudo em uma única transação, removendo geração anterior se existir
            try (Connection conn = new PostgresConnection().getConnection()) {
                final List<FolhaPagamento> finalLancamentos = lancamentos;
                TransacaoUtil.executar(conn, c -> {
                    folhaDAO.deletarPorPeriodo(periodo, c);
                    for (FolhaPagamento fp : finalLancamentos) {
                        folhaDAO.inserirLancamento(fp, c);
                    }
                });
            }

            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            result.put("msg", "Folha gerada para " + lancamentos.size() + " funcionários.");
            result.put("total", lancamentos.size());
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = resp.getWriter()) {
                out.print(gson.toJson(result));
                out.flush();
            }

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControleColaborador.class, "Falha tratada em ControleColaborador.", e);
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao gerar folha: " + cod, "modal");
        }
    }

    private void handleListarFolha(JsonObject jo, HttpServletResponse resp) throws IOException {
        String periodo = getStr(jo, "periodo");
        if (isBlank(periodo)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Período é obrigatório.", "modal");
            return;
        }
        try {
            List<FolhaPagamento> lista = folhaDAO.listByPeriodo(periodo);
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = resp.getWriter()) {
                out.print(gson.toJson(lista));
                out.flush();
            }
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControleColaborador.class, "Falha tratada em ControleColaborador.", e);
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao listar folha: " + cod, "modal");
        }
    }

    private BigDecimal buscarValorDiarista(int idFuncionario) throws SQLException {
        String sql = "SELECT valorpordia FROM funcionariodiarista WHERE idpessoa=?";
        try (Connection conn = new PostgresConnection().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal("valorpordia");
                    return v != null ? v : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal buscarValorEmpreita(int idFuncionario) throws SQLException {
        String sql = "SELECT valorfixoacordado FROM funcionarioempreita WHERE idpessoa=?";
        try (Connection conn = new PostgresConnection().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal("valorfixoacordado");
                    return v != null ? v : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal[] buscarDadosProducao(int idFuncionario, String periodo) throws SQLException {
        BigDecimal valorPorUnidade = BigDecimal.ZERO;
        BigDecimal quantidade = BigDecimal.ZERO;

        String sqlValor = "SELECT valorporunidade FROM funcionarioproducao WHERE idpessoa=?";
        try (Connection conn = new PostgresConnection().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sqlValor)) {
            stmt.setInt(1, idFuncionario);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal("valorporunidade");
                    valorPorUnidade = v != null ? v : BigDecimal.ZERO;
                }
            }
        }

        String sqlQtd = "SELECT COALESCE(SUM(quantidadeproduzida),0) as total FROM lancamento_producao "
                + "WHERE idfuncionario=? AND periodo=?";
        try (Connection conn = new PostgresConnection().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sqlQtd)) {
            stmt.setInt(1, idFuncionario);
            stmt.setString(2, periodo);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) quantidade = rs.getBigDecimal("total");
            }
        }

        return new BigDecimal[]{quantidade, valorPorUnidade};
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String getStr(JsonObject jo, String campo) {
        return jo.has(campo) && !jo.get(campo).isJsonNull() ? jo.get(campo).getAsString() : null;
    }
}
