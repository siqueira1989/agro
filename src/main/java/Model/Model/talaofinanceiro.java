
package Model.Model;

import java.util.Date;

public class TalaoFinanceiro {

    private int idTalaoFinanceiro;
    private int idTalao;
    private Talao talaoTalaoFinanceiro;
    private String safraTalaoFinanceiro;
    private Date iniciosafraTalaoFinanceiro;
    private Date terminosafraTalaoFinanceiro;
    private Double custosafraTalaoFinanceiro;
    private Double despesassafraTalaoFinanceiro;
    private Double vendabrutassafraTalaoFinanceiro;
    private Double vendasliquidassafraTalaoFinanceiro;

    public int getIdTalaoFinanceiro() {
        return idTalaoFinanceiro;
    }

    public void setIdTalaoFinanceiro(int idTalaoFinanceiro) {
        this.idTalaoFinanceiro = idTalaoFinanceiro;
    }

    public int getIdTalao() { return idTalao; }
    public void setIdTalao(int idTalao) { this.idTalao = idTalao; }

    public Talao getTalaoTalaoFinanceiro() {
        return talaoTalaoFinanceiro;
    }

    public void setTalaoTalaoFinanceiro(Talao talaoTalaoFinanceiro) {
        this.talaoTalaoFinanceiro = talaoTalaoFinanceiro;
    }

    public String getSafraTalaoFinanceiro() {
        return safraTalaoFinanceiro;
    }

    public void setSafraTalaoFinanceiro(String safraTalaoFinanceiro) {
        this.safraTalaoFinanceiro = safraTalaoFinanceiro;
    }

    public Date getIniciosafraTalaoFinanceiro() {
        return iniciosafraTalaoFinanceiro;
    }

    public void setIniciosafraTalaoFinanceiro(Date iniciosafraTalaoFinanceiro) {
        this.iniciosafraTalaoFinanceiro = iniciosafraTalaoFinanceiro;
    }

    public Date getTerminosafraTalaoFinanceiro() {
        return terminosafraTalaoFinanceiro;
    }

    public void setTerminosafraTalaoFinanceiro(Date terminosafraTalaoFinanceiro) {
        this.terminosafraTalaoFinanceiro = terminosafraTalaoFinanceiro;
    }

    public Double getCustosafraTalaoFinanceiro() {
        return custosafraTalaoFinanceiro;
    }

    public void setCustosafraTalaoFinanceiro(Double custosafraTalaoFinanceiro) {
        this.custosafraTalaoFinanceiro = custosafraTalaoFinanceiro;
    }

    public Double getDespesassafraTalaoFinanceiro() {
        return despesassafraTalaoFinanceiro;
    }

    public void setDespesassafraTalaoFinanceiro(Double despesassafraTalaoFinanceiro) {
        this.despesassafraTalaoFinanceiro = despesassafraTalaoFinanceiro;
    }

    public Double getVendabrutassafraTalaoFinanceiro() {
        return vendabrutassafraTalaoFinanceiro;
    }

    public void setVendabrutassafraTalaoFinanceiro(Double vendabrutassafraTalaoFinanceiro) {
        this.vendabrutassafraTalaoFinanceiro = vendabrutassafraTalaoFinanceiro;
    }

    public Double getVendasliquidassafraTalaoFinanceiro() {
        return vendasliquidassafraTalaoFinanceiro;
    }

    public void setVendasliquidassafraTalaoFinanceiro(Double vendasliquidassafraTalaoFinanceiro) {
        this.vendasliquidassafraTalaoFinanceiro = vendasliquidassafraTalaoFinanceiro;
    }

}
