/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Model;


public class Alimento extends Produto {

    private String variedadealimento;

   
   

    public String getVariedadealimento() {
        return variedadealimento;
    }

    public void setVariedadealimento(String variedadealimento) {
        this.variedadealimento = variedadealimento;
    }

  public Alimento(){}

    public Alimento(String variedadealimento, int idproduto, String nomeproduto, double quantidadeproduto, double valorunitarioproduto, double valorvendaproduto, boolean situacaoproduto, String tipoproduto) {
        super(idproduto, nomeproduto, quantidadeproduto, valorunitarioproduto, valorvendaproduto, situacaoproduto, tipoproduto);
        this.variedadealimento = variedadealimento;
       
    }
 
}
