package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Romaneio (V33): venda das caixas de um lote de colheita a um parceiro comprador.
 * Situação: ABERTO (falta preço) → PENDENTE (todos os itens com preço) → FINALIZADO (líquido pago);
 * CANCELADO libera as caixas do lote.
 */
public class Romaneio {
    public static final String ABERTO = "ABERTO", PENDENTE = "PENDENTE", FINALIZADO = "FINALIZADO", CANCELADO = "CANCELADO";

    private int idRomaneio;
    private String numero;
    private int idLote;            private String numeroDiario;   // diário de Colheita do lote (exibição)
    private int idArea;            private String nomeArea;       private String siglaArea;
    private int idQuadra;          private String nomeQuadra;
    private int idAlimento;        private String nomeAlimento;
    private Integer idSafra;       private String nomeSafra;
    private int idParceiro;        private String nomeParceiro;
    private LocalDate dataEmissao;
    private LocalDate dataColheita;
    private String motorista;
    private String placa;
    private String status = ABERTO;
    private BigDecimal totalCaixas = BigDecimal.ZERO;
    private BigDecimal valorBruto = BigDecimal.ZERO;
    private BigDecimal totalDespesas = BigDecimal.ZERO;
    private BigDecimal valorLiquido = BigDecimal.ZERO;
    private BigDecimal valorPago = BigDecimal.ZERO;
    private String observacao;
    private List<RomaneioItem> itens = new ArrayList<>();
    private List<RomaneioDespesa> despesas = new ArrayList<>();

    public BigDecimal getSaldo() { return valorLiquido.subtract(valorPago); }

    public int getIdRomaneio() { return idRomaneio; }
    public void setIdRomaneio(int idRomaneio) { this.idRomaneio = idRomaneio; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public int getIdLote() { return idLote; }
    public void setIdLote(int idLote) { this.idLote = idLote; }
    public String getNumeroDiario() { return numeroDiario; }
    public void setNumeroDiario(String numeroDiario) { this.numeroDiario = numeroDiario; }
    public int getIdArea() { return idArea; }
    public void setIdArea(int idArea) { this.idArea = idArea; }
    public String getNomeArea() { return nomeArea; }
    public void setNomeArea(String nomeArea) { this.nomeArea = nomeArea; }
    public String getSiglaArea() { return siglaArea; }
    public void setSiglaArea(String siglaArea) { this.siglaArea = siglaArea; }
    public int getIdQuadra() { return idQuadra; }
    public void setIdQuadra(int idQuadra) { this.idQuadra = idQuadra; }
    public String getNomeQuadra() { return nomeQuadra; }
    public void setNomeQuadra(String nomeQuadra) { this.nomeQuadra = nomeQuadra; }
    public int getIdAlimento() { return idAlimento; }
    public void setIdAlimento(int idAlimento) { this.idAlimento = idAlimento; }
    public String getNomeAlimento() { return nomeAlimento; }
    public void setNomeAlimento(String nomeAlimento) { this.nomeAlimento = nomeAlimento; }
    public Integer getIdSafra() { return idSafra; }
    public void setIdSafra(Integer idSafra) { this.idSafra = idSafra; }
    public String getNomeSafra() { return nomeSafra; }
    public void setNomeSafra(String nomeSafra) { this.nomeSafra = nomeSafra; }
    public int getIdParceiro() { return idParceiro; }
    public void setIdParceiro(int idParceiro) { this.idParceiro = idParceiro; }
    public String getNomeParceiro() { return nomeParceiro; }
    public void setNomeParceiro(String nomeParceiro) { this.nomeParceiro = nomeParceiro; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public LocalDate getDataColheita() { return dataColheita; }
    public void setDataColheita(LocalDate dataColheita) { this.dataColheita = dataColheita; }
    public String getMotorista() { return motorista; }
    public void setMotorista(String motorista) { this.motorista = motorista; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalCaixas() { return totalCaixas; }
    public void setTotalCaixas(BigDecimal totalCaixas) { this.totalCaixas = totalCaixas; }
    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }
    public BigDecimal getTotalDespesas() { return totalDespesas; }
    public void setTotalDespesas(BigDecimal totalDespesas) { this.totalDespesas = totalDespesas; }
    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public List<RomaneioItem> getItens() { return itens; }
    public void setItens(List<RomaneioItem> itens) { this.itens = itens; }
    public List<RomaneioDespesa> getDespesas() { return despesas; }
    public void setDespesas(List<RomaneioDespesa> despesas) { this.despesas = despesas; }
}
