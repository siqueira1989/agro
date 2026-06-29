
package Model.Model;


public class Talao {
    private int idTalao;
    private String descricaoTalao;
    private Alimento alimentoTalao;
    private AreaProducao areaproducaoTalao;
    private int quantidadeplantasTalao; 

    public int getIdTalao() {
        return idTalao;
    }

    public void setIdTalao(int idTalao) {
        this.idTalao = idTalao;
    }

    public String getDescricaoTalao() {
        return descricaoTalao;
    }

    public void setDescricaoTalao(String descricaoTalao) {
        this.descricaoTalao = descricaoTalao;
    }

    public Alimento getAlimentoTalao() {
        return alimentoTalao;
    }

    public void setAlimentoTalao(Alimento alimentoTalao) {
        this.alimentoTalao = alimentoTalao;
    }

    public AreaProducao getAreaproducaoTalao() {
        return areaproducaoTalao;
    }

    public void setAreaproducaoTalao(AreaProducao areaproducaoTalao) {
        this.areaproducaoTalao = areaproducaoTalao;
    }

    public int getQuantidadeplantasTalao() {
        return quantidadeplantasTalao;
    }

    public void setQuantidadeplantasTalao(int quantidadeplantasTalao) {
        this.quantidadeplantasTalao = quantidadeplantasTalao;
    }
    
}
