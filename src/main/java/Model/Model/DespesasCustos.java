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

    /* V32 — despesas de venda do Romaneio. Em PERCENTUAL_VENDA, valordespesascustos é o % (18 = 18%). */
    public static final String FORMA_FIXO = "FIXO";
    public static final String FORMA_PERCENTUAL_VENDA = "PERCENTUAL_VENDA";
    public static final String FORMA_POR_CAIXA = "POR_CAIXA";

    private String formaCalculo = FORMA_FIXO;
    private boolean aplicaRomaneio;

    public static boolean isFormaCalculoValida(String f) {
        return FORMA_FIXO.equals(f) || FORMA_PERCENTUAL_VENDA.equals(f) || FORMA_POR_CAIXA.equals(f);
    }

    public String getFormaCalculo() { return formaCalculo; }
    public void setFormaCalculo(String formaCalculo) { this.formaCalculo = formaCalculo; }

    public boolean isAplicaRomaneio() { return aplicaRomaneio; }
    public void setAplicaRomaneio(boolean aplicaRomaneio) { this.aplicaRomaneio = aplicaRomaneio; }

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
