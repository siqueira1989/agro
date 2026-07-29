package Model.Dao;

import Model.Model.TipoAtividade;
import Util.PostgresConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Tipos de atividade do Diário (lookup extensível). */
public class TipoAtividadeDAO {

    public List<TipoAtividade> listar(boolean somenteAtivos) throws SQLException {
        String sql = "SELECT * FROM diario_tipo_atividade"
                   + (somenteAtivos ? " WHERE ativo=true" : "") + " ORDER BY nome";
        List<TipoAtividade> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                TipoAtividade t = new TipoAtividade();
                t.setIdTipo(rs.getInt("idtipo"));
                t.setNome(rs.getString("nome"));
                t.setAtivo(rs.getBoolean("ativo"));
                lista.add(t);
            }
        }
        return lista;
    }

    /** Insere um novo tipo (idempotente por nome). Retorna o id existente/novo. */
    public int inserir(String nome) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            try (PreparedStatement sel = c.prepareStatement("SELECT idtipo FROM diario_tipo_atividade WHERE lower(nome)=lower(?)")) {
                sel.setString(1, nome);
                ResultSet rs = sel.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO diario_tipo_atividade (nome) VALUES (?) RETURNING idtipo")) {
                ins.setString(1, nome.trim());
                ResultSet rs = ins.executeQuery();
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean existe(int idTipo) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("SELECT 1 FROM diario_tipo_atividade WHERE idtipo=?")) {
            st.setInt(1, idTipo);
            return st.executeQuery().next();
        }
    }
}
