/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Dao;

import Model.Model.Funcionario;
import Model.Model.TipoFuncionario;


import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

  

public class FuncionarioDAO {

    // Retorna a tabela filho correta com base no tipo.
    // Seguro contra SQL injection: derivado do enum, nunca de input do usuário.
    private String resolverTabela(TipoFuncionario tipo) {
        if (tipo == null) return "funcionario";
        switch (tipo) {
            case CLT:      return "funcionarioclt";
            case DIARISTA: return "funcionariodiarista";
            case EMPREITA: return "funcionarioempreita";
            case PRODUCAO: return "funcionarioproducao";
            default:       return "funcionario";
        }
    }

    private void bindCampos(PreparedStatement stmt, Funcionario f) throws SQLException {
        stmt.setString(1, f.getNomePessoa());
        stmt.setString(2, f.getUsuarioPessoa());
        stmt.setString(3, Util.SenhaUtil.hashOuVazio(f.getSenhaPessoa()));  // P0-2
        stmt.setString(4, f.getNivelPessoa());
        stmt.setBoolean(5, f.isSituacaoPessoa());
        stmt.setString(6, f.getEmailPessoa());
        stmt.setString(7, f.getTelefonePessoa());
        stmt.setString(8, f.getCpfPf());
        if (f.getDataNascimentoPf() != null)
            stmt.setDate(9, java.sql.Date.valueOf(f.getDataNascimentoPf()));
        else
            stmt.setNull(9, java.sql.Types.DATE);
        stmt.setInt(10, f.getNumero());
        stmt.setString(11, f.getComplemento());
        stmt.setString(12, f.getCep());
        stmt.setString(13, f.getMatricula());
        if (f.getTipoFuncionario() != null)
            stmt.setObject(14, f.getTipoFuncionario().name(), java.sql.Types.OTHER);
        else
            stmt.setNull(14, java.sql.Types.OTHER);
        stmt.setString(15, f.getCargo());
        if (f.getDataInicio() != null)
            stmt.setDate(16, java.sql.Date.valueOf(f.getDataInicio()));
        else
            stmt.setNull(16, java.sql.Types.DATE);
        if (f.getDataFim() != null)
            stmt.setDate(17, java.sql.Date.valueOf(f.getDataFim()));
        else
            stmt.setNull(17, java.sql.Types.DATE);
    }

    public void insertFuncionario(Funcionario funcionario) throws SQLException {
        String tabela = resolverTabela(funcionario.getTipoFuncionario());
        // As colunas extras das tabelas filho (salariomensal, valorpordia, etc.)
        // possuem DEFAULT 0 no banco — não precisam ser informadas no INSERT.
        String sql = "INSERT INTO " + tabela + " ("
                + "nomepessoa, usuariopessoa, senhapessoa, nivelpessoa, "
                + "situacaopessoa, emailpessoa, telefonepessoa, cpfpf, "
                + "datanascimentopf, numero, complemento, cep, "
                + "matriculafuncionario, tipofuncionario, cargofuncionario, "
                + "datainiciofuncionario, datafimfuncionario"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            bindCampos(stmt, funcionario);
            stmt.executeUpdate();
        }
    }

    public void updateFuncionario(Funcionario funcionario) throws SQLException {
        // UPDATE via tabela pai funciona com herança PG: o PG roteia para a tabela
        // filho correta automaticamente ao fazer UPDATE na hierarquia.
        // P0-2: senha em branco no formulario mantem a atual, em vez de apaga-la.
        String sql = "UPDATE funcionario SET "
                + "nomepessoa = ?, usuariopessoa = ?, "
                + "senhapessoa = COALESCE(NULLIF(?, ''), senhapessoa), "
                + "nivelpessoa = ?, situacaopessoa = ?, emailpessoa = ?, "
                + "telefonepessoa = ?, cpfpf = ?, datanascimentopf = ?, "
                + "numero = ?, complemento = ?, cep = ?, "
                + "matriculafuncionario = ?, tipofuncionario = ?, "
                + "cargofuncionario = ?, datainiciofuncionario = ?, "
                + "datafimfuncionario = ? "
                + "WHERE idpessoa = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            bindCampos(stmt, funcionario);
            stmt.setInt(18, funcionario.getIdPessoa());
            stmt.executeUpdate();
        }
    }

    public void deleteFuncionario(int id, Boolean situacao) throws SQLException {
        String sql = "UPDATE funcionario SET situacaopessoa = ? WHERE idpessoa = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setBoolean(1, situacao != null && situacao);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    private Funcionario mapearFuncionario(ResultSet rs) throws SQLException {
        return new Funcionario(
                rs.getInt("idpessoa"),
                rs.getString("nomepessoa"),
                rs.getString("usuariopessoa"),
                null /* P0-2: a senha nunca sai do banco */,
                rs.getString("nivelpessoa"),
                rs.getBoolean("situacaopessoa"),
                rs.getString("emailpessoa"),
                rs.getString("telefonepessoa"),
                rs.getString("cep"),
                rs.getInt("numero"),
                rs.getString("complemento"),
                rs.getString("cpfpf"),
                rs.getDate("datanascimentopf") != null
                        ? rs.getDate("datanascimentopf").toLocalDate() : null,
                rs.getString("matriculafuncionario"),
                rs.getString("cargofuncionario"),
                rs.getString("tipofuncionario") != null
                        ? TipoFuncionario.valueOf(rs.getString("tipofuncionario")) : null,
                rs.getDate("datainiciofuncionario") != null
                        ? rs.getDate("datainiciofuncionario").toLocalDate() : null,
                rs.getDate("datafimfuncionario") != null
                        ? rs.getDate("datafimfuncionario").toLocalDate() : null
        );
    }

    public Funcionario getFuncionarioById(int id) throws SQLException {
        String sql = "SELECT * FROM funcionario WHERE idpessoa = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapearFuncionario(rs) : null;
            }
        }
    }

    public List<Funcionario> listAllFuncionario() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT * FROM funcionario ORDER BY nomepessoa";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFuncionario(rs));
        }
        return lista;
    }

    public List<Funcionario> listFuncionarioAtivo() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT * FROM funcionario WHERE situacaopessoa = true ORDER BY nomepessoa";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapearFuncionario(rs));
        }
        return lista;
    }

    public boolean existeCPF(String cpf) throws SQLException {
        String sql = "SELECT COUNT(*) FROM funcionario WHERE cpfpf = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existeMatricula(String matricula) throws SQLException {
        String sql = "SELECT COUNT(*) FROM funcionario WHERE matriculafuncionario = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, matricula);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Verifica em tempo real se um valor já existe (cpf | matricula | usuario),
     * opcionalmente ignorando um idpessoa (para o modo edição).
     * A coluna/tabela vêm de whitelist — seguro contra SQL injection.
     */
    public boolean existeValor(String campo, String valor, Integer excluirId) throws SQLException {
        String coluna, tabela;
        switch (campo == null ? "" : campo) {
            case "cpf":       coluna = "cpfpf";                tabela = "funcionario"; break;
            case "matricula": coluna = "matriculafuncionario"; tabela = "funcionario"; break;
            case "usuario":   coluna = "usuariopessoa";        tabela = "pessoa";      break;
            default: return false;
        }
        if (valor == null || valor.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM " + tabela + " WHERE " + coluna + " = ?"
                   + (excluirId != null ? " AND idpessoa <> ?" : "");
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, valor.trim());
            if (excluirId != null) stmt.setInt(2, excluirId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Funcionario buscarPorCPF(String cpf) throws SQLException {
        String sql = "SELECT * FROM funcionario WHERE cpfpf = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapearFuncionario(rs) : null;
            }
        }
    }
    
    public int contarPorSituacao(boolean ativo) throws SQLException {
        int numero = 0;

        String sql = "SELECT COUNT(*) FROM funcionario WHERE situacaopessoa = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setBoolean(1, ativo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                numero = rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao contar funcionarios por situação: " + e.getMessage());
            throw e;
        }

        return numero;
    }

    public int contarTodos() throws SQLException {
        int numero = 0;

        String sql = "SELECT COUNT(*) FROM funcionario";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                numero = rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao contar todos os funcionarios: " + e.getMessage());
            throw e;
        }

        return numero;
    }
    
    public List<Funcionario> listarPorTipoFuncionario(TipoFuncionario tipo) throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT * FROM funcionario WHERE tipofuncionario = ? ORDER BY nomepessoa";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            if (tipo != null)
                stmt.setObject(1, tipo.name(), java.sql.Types.OTHER);
            else
                stmt.setNull(1, java.sql.Types.OTHER);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearFuncionario(rs));
            }
        }
        return lista;
    }

    /** Colunas de valor específico por tipo (whitelist). */
    private String[] tabelaColunaValor(TipoFuncionario tipo) {
        if (tipo == null) return null;
        switch (tipo) {
            case DIARISTA: return new String[]{"funcionariodiarista", "valorpordia"};
            case EMPREITA: return new String[]{"funcionarioempreita", "valorfixoacordado"};
            case PRODUCAO: return new String[]{"funcionarioproducao", "valorporunidade"};
            default:       return null;
        }
    }

    /** Atualiza o valor específico do tipo (diária, empreita ou por unidade). */
    public void updateValorEspecifico(int idpessoa, TipoFuncionario tipo, BigDecimal valor) throws SQLException {
        String[] tc = tabelaColunaValor(tipo);
        if (tc == null || valor == null) return;
        String sql = "UPDATE " + tc[0] + " SET " + tc[1] + "=? WHERE idpessoa=?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setBigDecimal(1, valor);
            stmt.setInt(2, idpessoa);
            stmt.executeUpdate();
        }
    }

    /** Lê o valor específico do tipo (para o modo edição). */
    public BigDecimal getValorEspecifico(int idpessoa, TipoFuncionario tipo) throws SQLException {
        String[] tc = tabelaColunaValor(tipo);
        if (tc == null) return BigDecimal.ZERO;
        String sql = "SELECT " + tc[1] + " FROM " + tc[0] + " WHERE idpessoa=?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idpessoa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal(1);
                    return v != null ? v : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public Integer obterIdFuncionarioPorCPF(String cpf) throws SQLException {
        String sql = "SELECT idpessoa FROM funcionario WHERE cpfpf = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idpessoa") : null;
            }
        }
    }
}



       