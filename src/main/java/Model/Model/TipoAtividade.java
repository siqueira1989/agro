package Model.Model;

/** Tipo de atividade do Diário de Campo (lookup extensível). */
public class TipoAtividade {
    private int idTipo;
    private String nome;
    private boolean ativo = true;

    public int getIdTipo() { return idTipo; }
    public void setIdTipo(int idTipo) { this.idTipo = idTipo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
