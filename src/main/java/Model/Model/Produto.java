
package Model.Model;

public class Produto {
    private int idproduto;
    private String nomeproduto;
    private double quantidadeproduto;
    private double valorunitarioproduto;
    private double valorvendaproduto;
    private boolean situacaoproduto;
    private String tipoproduto;
 public Produto() { }
    public Produto(int idproduto, String nomeproduto, double quantidadeproduto, double valorunitarioproduto, double valorvendaproduto, boolean situacaoproduto, String tipoproduto) {
        this.idproduto = idproduto;
        this.nomeproduto = nomeproduto;
        this.quantidadeproduto = quantidadeproduto;
        this.valorunitarioproduto = valorunitarioproduto;
        this.valorvendaproduto = valorvendaproduto;
        this.situacaoproduto = situacaoproduto;
        this.tipoproduto = tipoproduto;
    }

    public int getIdproduto() {
        return idproduto;
    }

    public void setIdproduto(int idproduto) {
        this.idproduto = idproduto;
    }

    public String getNomeproduto() {
        return nomeproduto;
    }

    public void setNomeproduto(String nomeproduto) {
        this.nomeproduto = nomeproduto;
    }

    public double getQuantidadeproduto() {
        return quantidadeproduto;
    }

    public void setQuantidadeproduto(double quantidadeproduto) {
        this.quantidadeproduto = quantidadeproduto;
    }

    public double getValorunitarioproduto() {
        return valorunitarioproduto;
    }

    public void setValorunitarioproduto(double valorunitarioproduto) {
        this.valorunitarioproduto = valorunitarioproduto;
    }

    public double getValorvendaproduto() {
        return valorvendaproduto;
    }

    public void setValorvendaproduto(double valorvendaproduto) {
        this.valorvendaproduto = valorvendaproduto;
    }

    public boolean isSituacaoproduto() {
        return situacaoproduto;
    }

    public void setSituacaoproduto(boolean situacaoproduto) {
        this.situacaoproduto = situacaoproduto;
    }

    public String getTipoproduto() {
        return tipoproduto;
    }

    public void setTipoproduto(String tipoproduto) {
        this.tipoproduto = tipoproduto;
    }
    
    
}
