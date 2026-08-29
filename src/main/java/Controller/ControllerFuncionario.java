/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.Dao.FuncionarioDAO;
import Model.Dao.VinculoDAO;
import Model.Model.Funcionario;
import Model.Model.TipoFuncionario;
import Model.Model.Vinculo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerFuncionario extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioDAO funcionariodao;
    private VinculoDAO vinculodao;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext ctx) {
                    return new JsonPrimitive(src.toString()); // "yyyy-MM-dd"
                }
            })
            .create();

    @Override
    public void init() {
        funcionariodao = new FuncionarioDAO();
        vinculodao = new VinculoDAO();
    }

    /* ============================================================
       Helper: resposta JSON padronizada
       ============================================================ */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", ok);
        payload.put("msg", msg);
        if (target != null) {
            payload.put("target", target);
        }

        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }

    /* ============================================================
       POST: create | update | delete | buscarcpf | contar
       ============================================================ */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                json.append(linha);
            }
        }

        JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);

        if (jsonObject == null || !jsonObject.has("acao")) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Ação não informada.", "page");
            return;
        }

        String acao = jsonObject.get("acao").getAsString();

        switch (acao.toLowerCase()) {
            case "create":
                handleCreate(jsonObject, response);
                break;

            case "update":
                handleUpdate(jsonObject, response);
                break;

            case "delete":
                handleDelete(jsonObject, response);
                break;

            case "buscarcpf":
                handleBuscarPorCPF(jsonObject, response);
                break;

            case "obteridcpf":
                handleObterIdPorCPF(jsonObject, response);
                break;

            case "contagem":
                handleContagem(jsonObject, response);
                break;

            default:
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Ação inválida.", "page");
        }
    }

    /* ----------------------------- CREATE ----------------------------- */
    private void handleCreate(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            String nomePessoa = getAsString(jsonObject, "nomepessoa");
            String usuarioPessoa = getAsString(jsonObject, "usuariopessoa");
            String senhaPessoa = getAsString(jsonObject, "senhapessoa");
            String nivelPessoa = getAsString(jsonObject, "nivelpessoa");
            String emailPessoa = getAsString(jsonObject, "emailpessoa");
            String telefonePessoa = getAsString(jsonObject, "telefonepessoa");
            String cep = getAsString(jsonObject, "cep");
            String complemento = getAsString(jsonObject, "complemento");
            String cpfPf = getAsString(jsonObject, "cpfpf");
            String matricula = getAsString(jsonObject, "matriculafuncionario");
            String cargo = getAsString(jsonObject, "cargofuncionario");
            String tipoStr = getAsString(jsonObject, "tipofuncionario");

            Integer numero = getAsInteger(jsonObject, "numero");
            LocalDate dataNascimentoPf = getAsLocalDate(jsonObject, "datanascimentopf");
            LocalDate dataInicio = getAsLocalDate(jsonObject, "datainiciofuncionario");
            LocalDate dataFim = getAsLocalDate(jsonObject, "datafimfuncionario");
            java.math.BigDecimal valorEspecifico = getAsBigDecimal(jsonObject, "valorespecifico");
            java.math.BigDecimal salario = getAsBigDecimal(jsonObject, "salariofuncionario");

            if (isBlank(nomePessoa) || isBlank(cpfPf) || isBlank(matricula) || isBlank(cargo) || isBlank(tipoStr)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nome, CPF, matrícula, cargo e tipo funcionário são obrigatórios.",
                        "modalCadastroFuncionario");
                return;
            }

            if (dataNascimentoPf == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Data de nascimento é obrigatória.",
                        "modalCadastroFuncionario");
                return;
            }

            TipoFuncionario tipoFuncionario;
            try {
                tipoFuncionario = TipoFuncionario.valueOf(tipoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Tipo de funcionário inválido.", "modalCadastroFuncionario");
                return;
            }

            // Validações específicas por tipo
            String erroTipo = validarPorTipo(tipoFuncionario, cargo, valorEspecifico);
            if (erroTipo != null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, erroTipo, "modalCadastroFuncionario");
                return;
            }

            // ── Pessoa já existe (mesmo CPF) → READMISSÃO (novo vínculo) ──
            Integer idExistente = funcionariodao.obterIdFuncionarioPorCPF(cpfPf);
            if (idExistente != null) {
                Vinculo ativo = vinculodao.buscarAtivo(idExistente);
                if (ativo != null) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Este CPF já possui um vínculo ativo. Desligue o vínculo atual antes de readmitir.",
                            "modalCadastroFuncionario");
                    return;
                }
                // Reativa a pessoa-funcionário e atualiza os dados atuais
                Funcionario fEx = new Funcionario(idExistente, nomePessoa, usuarioPessoa,
                        senhaPessoa, nivelPessoa, true, emailPessoa, telefonePessoa, cep,
                        numero != null ? numero : 0, complemento, cpfPf, dataNascimentoPf,
                        matricula, cargo, tipoFuncionario, dataInicio, dataFim);
                funcionariodao.updateFuncionario(fEx);
                criarVinculo(idExistente, tipoFuncionario, cargo, dataInicio, dataFim, salario);
                if (valorEspecifico != null) {
                    funcionariodao.updateValorEspecifico(idExistente, tipoFuncionario, valorEspecifico);
                }

                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Funcionário readmitido com sucesso! Novo vínculo criado.", "page");
                return;
            }

            if (funcionariodao.existeMatricula(matricula)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Matrícula já cadastrada no sistema.", "modalCadastroFuncionario");
                return;
            }

            Funcionario funcionario = new Funcionario(
                    0,
                    nomePessoa,
                    usuarioPessoa,
                    senhaPessoa,
                    nivelPessoa,
                    true,
                    emailPessoa,
                    telefonePessoa,
                    cep,
                    numero != null ? numero : 0,
                    complemento,
                    cpfPf,
                    dataNascimentoPf,
                    matricula,
                    cargo,
                    tipoFuncionario,
                    dataInicio,
                    dataFim
            );

            funcionariodao.insertFuncionario(funcionario);

            // Cria o vínculo inicial da pessoa recém-cadastrada
            Integer novoId = funcionariodao.obterIdFuncionarioPorCPF(cpfPf);
            if (novoId != null) {
                criarVinculo(novoId, tipoFuncionario, cargo, dataInicio, dataFim, salario);
                if (valorEspecifico != null) {
                    funcionariodao.updateValorEspecifico(novoId, tipoFuncionario, valorEspecifico);
                }
            }

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Funcionário cadastrado com sucesso!", "page");

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar funcionário: " + cod, "modalCadastroFuncionario");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro inesperado ao cadastrar funcionário: " + cod, "modalCadastroFuncionario");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
    private void handleUpdate(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            Integer idPessoa = getAsInteger(jsonObject, "idpessoa");

            String nomePessoa = getAsString(jsonObject, "nomepessoa");
            String usuarioPessoa = getAsString(jsonObject, "usuariopessoa");
            String senhaPessoa = getAsString(jsonObject, "senhapessoa");
            String nivelPessoa = getAsString(jsonObject, "nivelpessoa");
            String emailPessoa = getAsString(jsonObject, "emailpessoa");
            String telefonePessoa = getAsString(jsonObject, "telefonepessoa");
            String cep = getAsString(jsonObject, "cep");
            String complemento = getAsString(jsonObject, "complemento");
            String cpfPf = getAsString(jsonObject, "cpfpf");
            String matricula = getAsString(jsonObject, "matriculafuncionario");
            String cargo = getAsString(jsonObject, "cargofuncionario");
            String tipoStr = getAsString(jsonObject, "tipofuncionario");

            Integer numero = getAsInteger(jsonObject, "numero");
            Boolean situacaoPessoa = getAsBoolean(jsonObject, "situacaopessoa");
            LocalDate dataNascimentoPf = getAsLocalDate(jsonObject, "datanascimentopf");
            LocalDate dataInicio = getAsLocalDate(jsonObject, "datainiciofuncionario");
            LocalDate dataFim = getAsLocalDate(jsonObject, "datafimfuncionario");
            java.math.BigDecimal valorEspecifico = getAsBigDecimal(jsonObject, "valorespecifico");

            if (idPessoa == null || isBlank(nomePessoa) || isBlank(cpfPf) || isBlank(matricula)
                    || isBlank(cargo) || isBlank(tipoStr)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID, nome, CPF, matrícula, cargo e tipo funcionário são obrigatórios.",
                        "modalAtualizarFuncionario");
                return;
            }

            if (dataNascimentoPf == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Data de nascimento é obrigatória.",
                        "modalAtualizarFuncionario");
                return;
            }

            TipoFuncionario tipoFuncionario;
            try {
                tipoFuncionario = TipoFuncionario.valueOf(tipoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Tipo de funcionário inválido.", "modalAtualizarFuncionario");
                return;
            }

            String erroTipo = validarPorTipo(tipoFuncionario, cargo, valorEspecifico);
            if (erroTipo != null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, erroTipo, "modalAtualizarFuncionario");
                return;
            }

            Funcionario existente = funcionariodao.getFuncionarioById(idPessoa);
            if (existente == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Funcionário não encontrado.", "page");
                return;
            }

            Integer idPorCpf = funcionariodao.obterIdFuncionarioPorCPF(cpfPf);
            if (idPorCpf != null && idPorCpf != idPessoa) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "CPF já pertence a outro funcionário.", "modalAtualizarFuncionario");
                return;
            }

            if (!existente.getMatricula().equalsIgnoreCase(matricula) && funcionariodao.existeMatricula(matricula)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Matrícula já cadastrada para outro funcionário.", "modalAtualizarFuncionario");
                return;
            }

            Funcionario funcionario = new Funcionario(
                    idPessoa,
                    nomePessoa,
                    usuarioPessoa,
                    senhaPessoa,
                    nivelPessoa,
                    situacaoPessoa != null ? situacaoPessoa : true,
                    emailPessoa,
                    telefonePessoa,
                    cep,
                    numero != null ? numero : 0,
                    complemento,
                    cpfPf,
                    dataNascimentoPf,
                    matricula,
                    cargo,
                    tipoFuncionario,
                    dataInicio,
                    dataFim
            );

            funcionariodao.updateFuncionario(funcionario);
            if (valorEspecifico != null) {
                funcionariodao.updateValorEspecifico(idPessoa, tipoFuncionario, valorEspecifico);
            }

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Funcionário atualizado com sucesso!", "page");

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar funcionário: " + cod, "modalAtualizarFuncionario");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro inesperado ao atualizar funcionário: " + cod, "modalAtualizarFuncionario");
        }
    }

    /* ----------------------------- DELETE / DESATIVAR ----------------------------- */
    private void handleDelete(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            Integer id = getAsInteger(jsonObject, "id");
            Boolean situacao = getAsBoolean(jsonObject, "situacao");

            if (id == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID é obrigatório para desativar/ativar.", "modalFuncionarioExcluir");
                return;
            }

            if (situacao == null) {
                situacao = false;
            }

            Funcionario funcionario = funcionariodao.getFuncionarioById(id);
            if (funcionario == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Funcionário não encontrado.", "page");
                return;
            }

            funcionariodao.deleteFuncionario(id, situacao);

            String mensagem = situacao
                    ? "Funcionário reativado com sucesso!"
                    : "Funcionário desativado com sucesso!";

            writeJson(response, HttpServletResponse.SC_OK, true, mensagem, "page");

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao alterar situação do funcionário: " + cod, "modalFuncionarioExcluir");
        }
    }

    /* ----------------------------- BUSCAR POR CPF ----------------------------- */
    private void handleBuscarPorCPF(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {
            String cpf = getAsString(jsonObject, "cpfpf");

            if (isBlank(cpf)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "CPF não informado.", "page");
                return;
            }

            Funcionario funcionario = funcionariodao.buscarPorCPF(cpf);

            if (funcionario != null) {
                out.print(gson.toJson(funcionario));
                out.flush();
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Funcionário não encontrado para o CPF informado.", "page");
            }

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao buscar funcionário por CPF: " + cod, "page");
        }
    }

    /* ----------------------------- OBTER ID POR CPF ----------------------------- */
    private void handleObterIdPorCPF(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {
            String cpf = getAsString(jsonObject, "cpfpf");

            if (isBlank(cpf)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "CPF não informado.", "page");
                return;
            }

            Integer id = funcionariodao.obterIdFuncionarioPorCPF(cpf);

            if (id != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("ok", true);
                payload.put("idpessoa", id);
                out.print(gson.toJson(payload));
                out.flush();
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nenhum funcionário encontrado para o CPF informado.", "page");
            }

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao obter ID do funcionário: " + cod, "page");
        }
    }

    /* ----------------------------- CONTAGEM ----------------------------- */
    private void handleContagem(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {
            String tipo = getAsString(jsonObject, "tipo");

            Map<String, Object> payload = new HashMap<>();
            payload.put("ok", true);

            if ("todos".equalsIgnoreCase(tipo)) {
                payload.put("total", funcionariodao.contarTodos());
            } else if ("ativos".equalsIgnoreCase(tipo)) {
                payload.put("total", funcionariodao.contarPorSituacao(true));
            } else if ("inativos".equalsIgnoreCase(tipo)) {
                payload.put("total", funcionariodao.contarPorSituacao(false));
            } else {
                payload.put("total", funcionariodao.contarTodos());
                payload.put("ativos", funcionariodao.contarPorSituacao(true));
                payload.put("inativos", funcionariodao.contarPorSituacao(false));
            }

            out.print(gson.toJson(payload));
            out.flush();

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao contar funcionários: " + cod, "page");
        }
    }

    /* ============================================================
       GET: listar | buscar por id | buscar por cpf | filtrar por tipo
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {

            String id = request.getParameter("id");
            String cpf = request.getParameter("cpf");
            String tipo = request.getParameter("tipo");
            String somenteAtivos = request.getParameter("ativos");

            // Verificação em tempo real de duplicidade (cpf | matricula | usuario)
            String existeCampo = request.getParameter("existe");
            if (existeCampo != null) {
                String valor = request.getParameter("valor");
                Integer excluir = null;
                String exStr = request.getParameter("excluir");
                if (exStr != null && !exStr.isBlank()) {
                    try { excluir = Integer.parseInt(exStr.trim()); } catch (NumberFormatException ignored) { }
                }
                boolean existe = funcionariodao.existeValor(existeCampo, valor, excluir);
                out.print("{\"existe\":" + existe + "}");
                out.flush();
                return;
            }

            if (id != null && !id.isEmpty()) {
                int idPessoa = Integer.parseInt(id);
                Funcionario funcionario = funcionariodao.getFuncionarioById(idPessoa);

                if (funcionario != null) {
                    // inclui o valor específico do tipo (diária/empreita/unidade) p/ edição
                    JsonObject jo = gson.toJsonTree(funcionario).getAsJsonObject();
                    jo.addProperty("valorEspecifico",
                            funcionariodao.getValorEspecifico(idPessoa, funcionario.getTipoFuncionario()));
                    out.print(gson.toJson(jo));
                } else {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Funcionário não encontrado.", "page");
                }
                return;
            }

            if (cpf != null && !cpf.isEmpty()) {
                Funcionario funcionario = funcionariodao.buscarPorCPF(cpf);

                if (funcionario != null) {
                    out.print(gson.toJson(funcionario));
                } else {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Funcionário não encontrado.", "page");
                }
                return;
            }

            if (tipo != null && !tipo.isEmpty()) {
                TipoFuncionario tipoFuncionario = TipoFuncionario.valueOf(tipo.toUpperCase());
                List<Funcionario> funcionarios = funcionariodao.listarPorTipoFuncionario(tipoFuncionario);
                out.print(gson.toJson(funcionarios));
                out.flush();
                return;
            }

            if ("true".equalsIgnoreCase(somenteAtivos)) {
                List<Funcionario> funcionarios = funcionariodao.listFuncionarioAtivo();
                out.print(gson.toJson(funcionarios));
                out.flush();
                return;
            }

            List<Funcionario> funcionarios = funcionariodao.listAllFuncionario();
            out.print(gson.toJson(funcionarios));
            out.flush();

        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tipo de funcionário inválido.", "page");
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerFuncionario.class, "Falha tratada em ControllerFuncionario.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar funcionários: " + cod, "page");
        }
    }

    /* ============================================================
       Helpers
       ============================================================ */
    /** Cria um vínculo empregatício (período de trabalho) para a pessoa. */
    private void criarVinculo(int idPessoa, TipoFuncionario tipo, String cargo,
                              LocalDate admissao, LocalDate desligamento,
                              java.math.BigDecimal salario) throws SQLException {
        Vinculo v = new Vinculo();
        v.setIdPessoa(idPessoa);
        v.setTipoFuncionario(tipo);
        v.setCargo(cargo);
        v.setDataAdmissao(admissao);
        v.setDataDesligamento(desligamento);
        v.setStatus(desligamento != null ? "DESLIGADO" : "ATIVO");
        // Salário mensal só se aplica ao CLT; demais tipos usam valor específico (diária/empreita).
        if (tipo == TipoFuncionario.CLT && salario != null) {
            v.setSalarioMensal(salario);
        }
        vinculodao.inserir(v);
    }

    /** Valida os campos específicos por tipo. Retorna a mensagem de erro ou null. */
    private String validarPorTipo(TipoFuncionario tipo, String cargo, java.math.BigDecimal valor) {
        if (tipo == TipoFuncionario.DIARISTA
                && (valor == null || valor.compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            return "Informe o valor da diária.";
        }
        if (tipo == TipoFuncionario.EMPREITA
                && (valor == null || valor.compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            return "Informe o valor da empreita.";
        }
        if (tipo == TipoFuncionario.PRODUCAO
                && !("Colhedor".equalsIgnoreCase(cargo) || "Embalador".equalsIgnoreCase(cargo))) {
            return "Cargo do funcionário de produção deve ser Colhedor ou Embalador.";
        }
        return null;
    }

    private java.math.BigDecimal getAsBigDecimal(JsonObject jsonObject, String campo) {
        try {
            if (!jsonObject.has(campo) || jsonObject.get(campo).isJsonNull()) return null;
            String s = jsonObject.get(campo).getAsString().trim();
            return s.isEmpty() ? null : new java.math.BigDecimal(s);
        } catch (Exception e) { return null; }
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String getAsString(JsonObject jsonObject, String campo) {
        return jsonObject.has(campo) && !jsonObject.get(campo).isJsonNull()
                ? jsonObject.get(campo).getAsString()
                : null;
    }

    private Integer getAsInteger(JsonObject jsonObject, String campo) {
        try {
            return jsonObject.has(campo) && !jsonObject.get(campo).isJsonNull()
                    && !jsonObject.get(campo).getAsString().trim().isEmpty()
                    ? jsonObject.get(campo).getAsInt()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getAsBoolean(JsonObject jsonObject, String campo) {
        try {
            return jsonObject.has(campo) && !jsonObject.get(campo).isJsonNull()
                    ? jsonObject.get(campo).getAsBoolean()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getAsLocalDate(JsonObject jsonObject, String campo) {
        try {
            String valor = getAsString(jsonObject, campo);
            return (valor != null && !valor.trim().isEmpty()) ? LocalDate.parse(valor) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

