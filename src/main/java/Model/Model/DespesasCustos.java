/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Model;

/**
 *
 * @author lucia
 */
public class DespesasCustos {
    private int iddespesascusto;
 private String despesascusto;
 private String unidadedespesascustos;
 private double valordespesascustos;
 private String tipodespesascustos;
 private String classificacao;

 /* V28 — preço de caixa/embalagem por alimento e área (categoria ≠ GERAL). */
 private String categoria = CATEGORIA_GERAL;
 private Integer idAlimento;
 private Integer idAreaProducao;
 private String tipoCaixa;
 /* Somente leitura (JOIN), para exibição: */
 private String nomeAlimento;
 private String nomeArea;
 private String siglaArea;

    public static final String CATEGORIA_GERAL = "GERAL";
    public static final String CATEGORIA_CAIXA_COLHEITA = "CAIXA_COLHEITA";
    public static final String CATEGORIA_EMBALAGEM = "EMBALAGEM";

    public static boolean isCategoriaValida(String c) {
        return CATEGORIA_GERAL.equals(c) || CATEGORIA_CAIXA_COLHEITA.equals(c) || CATEGORIA_EMBALAGEM.equals(c);
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getIdAlimento() { return idAlimento; }
    public void setIdAlimento(Integer idAlimento) { this.idAlimento = idAlimento; }

    public Integer getIdAreaProducao() { return idAreaProducao; }
    public void setIdAreaProducao(Integer idAreaProducao) { this.idAreaProducao = idAreaProducao; }

    public String getTipoCaixa() { return tipoCaixa; }
    public void setTipoCaixa(String tipoCaixa) { this.tipoCaixa = tipoCaixa; }

    public String getNomeAlimento() { return nomeAlimento; }
    public void setNomeAlimento(String nomeAlimento) { this.nomeAlimento = nomeAlimento; }

    public String getNomeArea() { return nomeArea; }
    public void setNomeArea(String nomeArea) { this.nomeArea = nomeArea; }

    public String getSiglaArea() { return siglaArea; }
    public void setSiglaArea(String siglaArea) { this.siglaArea = siglaArea; }

    public int getIddespesascusto() {
        return iddespesascusto;
    }

    public void setIddespesascusto(int iddespesascusto) {
        this.iddespesascusto = iddespesascusto;
    }

    public String getDespesascusto() {
        return despesascusto;
    }

    public void setDespesascusto(String despesascusto) {
        this.despesascusto = despesascusto;
    }

    public String getUnidadedespesascustos() {
        return unidadedespesascustos;
    }

    public void setUnidadedespesascustos(String unidadedespesascustos) {
        this.unidadedespesascustos = unidadedespesascustos;
    }

    public double getValordespesascustos() {
        return valordespesascustos;
    }

    public void setValordespesascustos(double valordespesascustos) {
        this.valordespesascustos = valordespesascustos;
    }

    public String getTipodespesascustos() {
        return tipodespesascustos;
    }

    public void setTipodespesascustos(String tipodespesascustos) {
        this.tipodespesascustos = tipodespesascustos;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

}
