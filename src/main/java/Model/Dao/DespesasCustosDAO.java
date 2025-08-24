/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Dao;

import Model.Model.DespesasCustos;
import Util.PostgresConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author lucia
 */
public class DespesasCustosDAO {

    public boolean existeDespesaCusto(String despesascustos) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();

        boolean existe = false;

        // Substitua pelo seu método de obter conexão com o banco de dados
        try {
            String sql = "SELECT COUNT(*) FROM despesascusto WHERE despesascusto = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            // Definindo o valor do parâmetro da consulta
            stmt.setString(1, despesascustos);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Verificando se a contagem é maior que zero
                existe = rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.out.println("Erro no nivel dao: " + e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
        }
        System.out.println(existe);
        return existe;
    }
    //Verificação de Atulização

    public boolean VerificacaoUpdateDespesaCusto(DespesasCustos despesaCusto) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();

        boolean existe = false;

        // Substitua pelo seu método de obter conexão com o banco de dados
        try {
            String sql = "SELECT COUNT(*) FROM despesascusto WHERE iddespesascusto = ? and despesascusto = ? and unidadedespesascustos = ? and valordespesascustos = ? and tipodespesascustos = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            // Definindo o valor do parâmetro da consulta
            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.setString(2, despesaCusto.getDespesascusto());
            stmt.setString(3, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(4, BigDecimal.valueOf(despesaCusto.getValordespesascustos()));
            stmt.setString(5, despesaCusto.getTipodespesascustos());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Verificando se a contagem é maior que zero
                existe = rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.out.println("Erro no nivel dao: " + e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
        }
        System.out.println("dao ok");
        return existe;
    }

    // Método para criar uma nova despesa/custo no banco de dados
    public void create(DespesasCustos despesaCusto) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        try {
            String sql = "INSERT INTO despesascusto (despesascusto, unidadedespesascustos, "
                    + "valordespesascustos, tipodespesascustos) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conexao.prepareStatement(sql);

            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, BigDecimal.valueOf(despesaCusto.getValordespesascustos()));
            stmt.setString(4, despesaCusto.getTipodespesascustos());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro no cadastro: " + e.getMessage());
        }
    }

    // Método para ler uma despesa/custo do banco de dados pelo id
    public DespesasCustos read(int id) throws SQLException {

        DespesasCustos despesaCusto = null;
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        try {
            String sql = "SELECT * FROM despesascusto WHERE iddespesascusto = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                despesaCusto = new DespesasCustos();
                despesaCusto.setIddespesascusto(rs.getInt("iddespesascusto"));
                despesaCusto.setDespesascusto(rs.getString("despesascusto"));
                despesaCusto.setUnidadedespesascustos(rs.getString("unidadedespesascustos"));
                despesaCusto.setValordespesascustos(rs.getBigDecimal("valordespesascustos").doubleValue());
                despesaCusto.setTipodespesascustos(rs.getString("tipodespesascustos"));
            }
        } catch (SQLException e) {
            System.out.println("Erro no cadastro: " + e.getMessage());
        }

        return despesaCusto;
    }

    // Método para atualizar uma despesa/custo no banco de dados
    public void update(DespesasCustos despesaCusto) throws SQLException {

        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        try {
            System.out.println("unidade2:");
            String sql = "UPDATE despesascusto SET despesascusto = ?, unidadedespesascustos = ?, "
                    + "valordespesascustos = ?, tipodespesascustos = ? WHERE iddespesascusto = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, BigDecimal.valueOf(despesaCusto.getValordespesascustos()));
            stmt.setString(4, despesaCusto.getTipodespesascustos());
            stmt.setInt(5, despesaCusto.getIddespesascusto());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro no cadastro: " + e.getMessage());
        }
    }

    // Método para deletar uma despesa/custo do banco de dados
    public void delete(DespesasCustos despesaCusto) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        try {
            String sql = "DELETE FROM despesascusto WHERE iddespesascusto = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro no cadastro: " + e.getMessage());
        }
    }

    // Método para listar todas as despesas/custos do banco de dados
    public List<DespesasCustos> listAll() throws SQLException {

        List<DespesasCustos> despesasCustosList = new ArrayList<>();
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        try {
            String sql = "SELECT * FROM despesascusto";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DespesasCustos despesaCusto = new DespesasCustos();
                despesaCusto.setIddespesascusto(rs.getInt("iddespesascusto"));
                despesaCusto.setDespesascusto(rs.getString("despesascusto"));
                despesaCusto.setUnidadedespesascustos(rs.getString("unidadedespesascustos"));
                despesaCusto.setValordespesascustos(rs.getBigDecimal("valordespesascustos").doubleValue());
                despesaCusto.setTipodespesascustos(rs.getString("tipodespesascustos"));

                despesasCustosList.add(despesaCusto);
            }
        } catch (SQLException e) {
            System.out.println("Erro no cadastro: " + e.getMessage());
        }

        return despesasCustosList;
    }

    public static String converterParaMoedaBrasileira(Double valor) {
        // Configurar o formatador para o Locale brasileiro
        @SuppressWarnings("deprecation")
        NumberFormat formatoBrasileiro = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        formatoBrasileiro.setMinimumFractionDigits(2);
        formatoBrasileiro.setMaximumFractionDigits(2);

        // Retorna o valor formatado
        return formatoBrasileiro.format(valor);
    }
}
