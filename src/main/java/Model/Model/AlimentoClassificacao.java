/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Dao;

import Model.Model.Alimento;
import Model.Model.Classificacao;

/**
 *
 * @author lucia
 */
public class AlimentoClassificacao {
    private int idalimentoclassificao;
    private Alimento alimento;
    private Classificacao classificacao;

    public int getIdalimentoclassificao() {
        return idalimentoclassificao;
    }

    public void setIdalimentoclassificao(int idalimentoclassificao) {
        this.idalimentoclassificao = idalimentoclassificao;
    }

    public Alimento getAlimento() {
        return alimento;
    }

    public void setAlimento(Alimento alimento) {
        this.alimento = alimento;
    }

    public Classificacao getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(Classificacao classificacao) {
        this.classificacao = classificacao;
    }
}
