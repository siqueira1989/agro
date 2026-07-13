package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um dia de empreita com valor (piecework). */
public class LancamentoEmpreita {

    private int        idLancamento;
    private int        idPessoa;
    private LocalDate  dataEmpreita;
    private String     descricao;
    private BigDecimal valor = BigDecimal.ZERO;
    private boolean    pago;

    public int getIdLancamento()               { return idLancamento; }
    public void setIdLancamento(int v)         { this.idLancamento = v; }

    public int getIdPessoa()                   { return idPessoa; }
    public void setIdPessoa(int v)             { this.idPessoa = v; }

    public LocalDate getDataEmpreita()         { return dataEmpreita; }
    public void setDataEmpreita(LocalDate v)   { this.dataEmpreita = v; }

    public String getDescricao()               { return descricao; }
    public void setDescricao(String v)         { this.descricao = v; }

    public BigDecimal getValor()               { return valor; }
    public void setValor(BigDecimal v)         { this.valor = v != null ? v : BigDecimal.ZERO; }

    public boolean isPago()                    { return pago; }
    public void setPago(boolean v)             { this.pago = v; }
}
