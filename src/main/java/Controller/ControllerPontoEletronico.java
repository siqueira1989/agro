package Controller;

import Model.Dao.*;
import Model.Model.*;
import Service.FolhaCalculoService;
import Util.EmailUtil;
import Util.PostgresConnection;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet("/ControllerPontoEletronico")
public class ControllerPontoEletronico extends HttpServlet {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (s, t, ctx) -> new JsonPrimitive(s.toString()))
            .registerTypeAdapter(LocalTime.class,
                    (JsonSerializer<LocalTime>) (s, t, ctx) -> new JsonPrimitive(
                            s.format(FMT_TIME)))
            .create();

    private final PontoEletronicoDAO pontoDAO  = new PontoEletronicoDAO();
    private final FaltaFuncionarioDAO faltaDAO = new FaltaFuncionarioDAO();
    private final ValeFuncionarioDAO  valeDAO  = new ValeFuncionarioDAO();
    private final FolhaCalculoService svc      = new FolhaCalculoService();

    // ── GET — retorna JSON para o frontend ───────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");

        String tipo      = req.getParameter("tipo");
        String idStr     = req.getParameter("idfuncionario");
        String periodo   = req.getParameter("periodo");
        PrintWriter out  = resp.getWriter();

        try {
            if ("funcionarios".equals(tipo)) {
                out.write(gson.toJson(listarFuncionariosAtivos()));
                return;
            }

            int id = (idStr != null && !idStr.isBlank()) ? Integer.parseInt(idStr) : 0;
            if (periodo == null || periodo.isBlank())
                periodo = YearMonth.now().toString();  // YYYY-MM

            switch (String.valueOf(tipo)) {
                case "pontos"     -> out.write(gson.toJson(pontoDAO.listarPorMes(id, periodo)));
                case "faltas"     -> out.write(gson.toJson(faltaDAO.listarPorMes(id, periodo)));
                case "vales"      -> out.write(gson.toJson(valeDAO.listarPorMes(id, periodo)));
                case "fechamento" -> {
                    FechamentoFolha f = svc.buscar(id, periodo);
                    out.write(f != null ? gson.toJson(f) : "null");
                }
                default -> {
                    resp.setStatus(400);
                    out.write("{\"ok\":false,\"msg\":\"Parâmetro tipo inválido.\"}");
                }
            }
        } catch (Exception e) {
            resp.setStatus(500);
            out.write("{\"ok\":false,\"msg\":\"Erro interno: " + esc(e.getMessage()) + "\"}");
        }
    }

    // ── POST — ações de escrita ───────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            JsonObject body = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String acao = body.get("acao").getAsString();

            switch (acao) {
                case "registrar_ponto"    -> out.write(gson.toJson(registrarPonto(body)));
                case "salvar_ponto_manual"-> out.write(gson.toJson(salvarPontoManual(body, req)));
                case "editar_ponto"       -> out.write(gson.toJson(editarPonto(body, req)));
                case "excluir_ponto"      -> out.write(gson.toJson(excluirPonto(body)));
                case "registrar_falta"    -> out.write(gson.toJson(registrarFalta(body)));
                case "excluir_falta"      -> out.write(gson.toJson(excluirFalta(body)));
                case "registrar_vale"     -> out.write(gson.toJson(registrarVale(body)));
                case "excluir_vale"       -> out.write(gson.toJson(excluirVale(body)));
                case "fechar_folha"       -> out.write(gson.toJson(fecharFolha(body)));
                default -> {
                    resp.setStatus(400);
                    out.write("{\"ok\":false,\"msg\":\"Ação desconhecida: " + esc(acao) + "\"}");
                }
            }
        } catch (Exception e) {
            resp.setStatus(500);
            out.write("{\"ok\":false,\"msg\":\"Erro: " + esc(e.getMessage()) + "\"}");
        }
    }

    // ── handlers ─────────────────────────────────────────────────────────────

    /**
     * Registra uma marcação usando o horário ATUAL do servidor.
     * campo: entrada1 | saida1 | entrada2 | saida2
     */
    private Map<String, Object> registrarPonto(JsonObject b) throws Exception {
        int       id    = b.get("idfuncionario").getAsInt();
        String    campo = b.get("campo").getAsString();
        LocalDate data  = b.has("dataregistro")
                ? LocalDate.parse(b.get("dataregistro").getAsString())
                : LocalDate.now();
        LocalTime hora  = b.has("hora") && !b.get("hora").isJsonNull()
                ? LocalTime.parse(b.get("hora").getAsString())
                : LocalTime.now().withSecond(0).withNano(0);

        PontoEletronico p = pontoDAO.registrarMarcacao(id, data, campo, hora);

        // e-mail de comprovante (assíncrono — não bloqueia resposta)
        new Thread(() -> EmailUtil.enviarComprovantePonto(
                p.getEmailFuncionario(), p.getNomeFuncionario(),
                labelCampo(campo), hora, data)).start();

        return Map.of("ok", true,
                "msg", "Ponto registrado: " + labelCampo(campo) + " às " + hora.format(FMT_TIME),
                "ponto", p);
    }

    /** Salva registro completo (lançamento manual pelo gestor/admin). */
    private Map<String, Object> salvarPontoManual(JsonObject b, HttpServletRequest req) throws Exception {
        PontoEletronico p = new PontoEletronico();
        p.setIdFuncionario(b.get("idfuncionario").getAsInt());
        p.setDataRegistro(LocalDate.parse(b.get("dataregistro").getAsString()));
        p.setEntrada1(parseTime(b, "entrada1"));
        p.setSaida1(parseTime(b, "saida1"));
        p.setEntrada2(parseTime(b, "entrada2"));
        p.setSaida2(parseTime(b, "saida2"));
        p.setObservacao(strOr(b, "observacao", ""));
        preencherAuditoria(p, req);
        pontoDAO.salvarRegistroCompleto(p);
        return Map.of("ok", true, "msg", "Registro salvo com sucesso.");
    }

    /** Preenche os dados de auditoria (Portaria 671) da ação do gestor. */
    private void preencherAuditoria(PontoEletronico p, HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        else ip = ip.split(",")[0].trim();
        p.setIpOrigem(ip);

        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) ua = ua.substring(0, 255);
        p.setUserAgent(ua);

        jakarta.servlet.http.HttpSession session = req.getSession(false);
        if (session != null) {
            Object u = session.getAttribute("usuarioLogado");
            if (u instanceof Model.Model.Pessoa) {
                p.setRegistradoPor(((Model.Model.Pessoa) u).getIdPessoa());
            }
        }
    }

    /** Edita um registro de ponto existente (correção de horários/data). */
    private Map<String, Object> editarPonto(JsonObject b, HttpServletRequest req) throws Exception {
        PontoEletronico p = new PontoEletronico();
        p.setIdPonto(b.get("idponto").getAsInt());
        p.setIdFuncionario(b.get("idfuncionario").getAsInt());
        p.setDataRegistro(LocalDate.parse(b.get("dataregistro").getAsString()));
        p.setEntrada1(parseTime(b, "entrada1"));
        p.setSaida1(parseTime(b, "saida1"));
        p.setEntrada2(parseTime(b, "entrada2"));
        p.setSaida2(parseTime(b, "saida2"));
        p.setObservacao(strOr(b, "observacao", ""));
        preencherAuditoria(p, req);
        try {
            pontoDAO.atualizarPorId(p);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_ponto_funcionario_dia")) {
                return Map.of("ok", false,
                        "msg", "Já existe um registro de ponto nessa data para este funcionário.");
            }
            throw e;
        }
        return Map.of("ok", true, "msg", "Registro atualizado com sucesso.");
    }

    private Map<String, Object> excluirPonto(JsonObject b) throws Exception {
        pontoDAO.excluir(b.get("idponto").getAsInt());
        return Map.of("ok", true, "msg", "Registro excluído.");
    }

    private Map<String, Object> registrarFalta(JsonObject b) throws Exception {
        FaltaFuncionario f = new FaltaFuncionario();
        f.setIdFuncionario(b.get("idfuncionario").getAsInt());
        f.setDataFalta(LocalDate.parse(b.get("datafalta").getAsString()));
        f.setJustificada(b.has("justificada") && b.get("justificada").getAsBoolean());
        f.setMotivo(strOr(b, "motivo", ""));
        faltaDAO.registrar(f);
        String tipo = f.isJustificada() ? "justificada" : "injustificada";
        return Map.of("ok", true, "msg", "Falta " + tipo + " registrada em " + f.getDataFalta() + ".");
    }

    private Map<String, Object> excluirFalta(JsonObject b) throws Exception {
        faltaDAO.excluir(b.get("idfalta").getAsInt());
        return Map.of("ok", true, "msg", "Falta removida.");
    }

    private Map<String, Object> registrarVale(JsonObject b) throws Exception {
        ValeFuncionario v = new ValeFuncionario();
        v.setIdFuncionario(b.get("idfuncionario").getAsInt());
        v.setDataVale(LocalDate.parse(b.get("datavale").getAsString()));
        v.setValor(b.get("valor").getAsBigDecimal());
        v.setDescricao(strOr(b, "descricao", ""));
        valeDAO.registrar(v);
        return Map.of("ok", true, "msg", "Vale de R$ " + v.getValor() + " registrado.");
    }

    private Map<String, Object> excluirVale(JsonObject b) throws Exception {
        valeDAO.excluir(b.get("idvale").getAsInt());
        return Map.of("ok", true, "msg", "Vale removido.");
    }

    private Map<String, Object> fecharFolha(JsonObject b) throws Exception {
        int        id          = b.get("idfuncionario").getAsInt();
        String     periodo     = b.get("periodo").getAsString();
        BigDecimal salarioBase = b.get("salario_base").getAsBigDecimal();
        int        jornada     = b.has("jornada_horas") ? b.get("jornada_horas").getAsInt() : 8;

        FechamentoFolha f = svc.calcularEPersistir(id, periodo, salarioBase, jornada);

        // busca e-mail do funcionário para envio do resumo
        String email = buscarEmail(id);
        String nome  = f.getNomeFuncionario() != null ? f.getNomeFuncionario() : "Funcionário";
        new Thread(() -> EmailUtil.enviarResumoFolha(
                email, nome, periodo,
                f.getSalarioBruto().toPlainString(),
                f.getSalarioLiquido().toPlainString(),
                f.getValorHorasExtras().toPlainString(),
                f.getTotalVales().toPlainString(),
                f.getDescontoFaltas().toPlainString())).start();

        return Map.of("ok", true,
                "msg", "Folha fechada com sucesso! Resumo enviado por e-mail.",
                "fechamento", f);
    }

    // ── utilidades ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> listarFuncionariosAtivos() throws SQLException {
        String sql = "SELECT f.idpessoa, p.nomepessoa, p.emailpessoa, f.tipofuncionario "
                + "FROM funcionario f "
                + "JOIN pessoafisica pf ON pf.idpessoa = f.idpessoa "
                + "JOIN pessoa p ON p.idpessoa = f.idpessoa "
                + "WHERE p.situacaopessoa = true "
                + "ORDER BY p.nomepessoa";
        List<Map<String, Object>> lista = new ArrayList<>();
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idPessoa",        rs.getInt("idpessoa"));
                m.put("nomePessoa",      rs.getString("nomepessoa"));
                m.put("emailPessoa",     rs.getString("emailpessoa"));
                m.put("tipoFuncionario", rs.getString("tipofuncionario"));
                lista.add(m);
            }
        }
        return lista;
    }

    private String buscarEmail(int id) throws SQLException {
        String sql = "SELECT emailpessoa FROM pessoa WHERE idpessoa = ?";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "";
    }

    private LocalTime parseTime(JsonObject b, String key) {
        if (!b.has(key) || b.get(key).isJsonNull()) return null;
        String v = b.get(key).getAsString().trim();
        return v.isBlank() ? null : LocalTime.parse(v);
    }

    private String strOr(JsonObject b, String key, String def) {
        return (b.has(key) && !b.get(key).isJsonNull()) ? b.get(key).getAsString() : def;
    }

    private String labelCampo(String campo) {
        return switch (campo) {
            case "entrada1" -> "Entrada 1";
            case "saida1"   -> "Saída 1";
            case "entrada2" -> "Entrada 2";
            case "saida2"   -> "Saída 2";
            default         -> campo;
        };
    }

    private String esc(String s) {
        return s == null ? "null" : s.replace("\"", "\\\"");
    }
}
