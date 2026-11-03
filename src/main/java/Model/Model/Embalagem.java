package Model.Model;

import java.math.BigDecimal;

/**
 * Embalagem do Processamento por alimento + área (módulo Embalagem, V31 + V34). O preço pago ao
 * embalador por caixa (Processar) fica aqui; o estoque fica no insumo ligado (categoria EMBALAGEM,
 * unidade CX): saldo, preço médio de compra, estoque mínimo e fornecedor.
 */
public class Embalagem {
    private int idEmbalagem;
    private String codigo;          // [A-Z0-9_]{1,30}, normalizado (EmbalagemDAO.normalizarCodigo)
    private String descricao;
    private BigDecimal pesoKg;
    private Integer idAlimento;     private String nomeAlimento;   // nome só leitura (JOIN)
    private Integer idArea;         private String nomeArea;       private String siglaArea;
    private BigDecimal precoEmbalador;
    private boolean ativo = true;

    // Estoque (insumo ligado, V34). precoCompra = insumo.preco_medio (só leitura; muda nas ENTRADAS).
    private Integer idInsumo;
    private BigDecimal precoCompra;
    private BigDecimal saldo;
    private BigDecimal estoqueMinimo;
    private Integer idFornecedor;   private String fornecedorNome;
    private boolean abaixoMinimo;

    public int getIdEmbalagem() { return idEmbalagem; }
    public void setIdEmbalagem(int idEmbalagem) { this.idEmbalagem = idEmbalagem; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public Integer getIdAlimento() { return idAlimento; }
    public void setIdAlimento(Integer idAlimento) { this.idAlimento = idAlimento; }
    public String getNomeAlimento() { return nomeAlimento; }
    public void setNomeAlimento(String nomeAlimento) { this.nomeAlimento = nomeAlimento; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public String getNomeArea() { return nomeArea; }
    public void setNomeArea(String nomeArea) { this.nomeArea = nomeArea; }
    public String getSiglaArea() { return siglaArea; }
    public void setSiglaArea(String siglaArea) { this.siglaArea = siglaArea; }
    public BigDecimal getPrecoEmbalador() { return precoEmbalador; }
    public void setPrecoEmbalador(BigDecimal precoEmbalador) { this.precoEmbalador = precoEmbalador; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Integer getIdInsumo() { return idInsumo; }
    public void setIdInsumo(Integer idInsumo) { this.idInsumo = idInsumo; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public void setPrecoCompra(BigDecimal precoCompra) { this.precoCompra = precoCompra; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public Integer getIdFornecedor() { return idFornecedor; }
    public void setIdFornecedor(Integer idFornecedor) { this.idFornecedor = idFornecedor; }
    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }
    public boolean isAbaixoMinimo() { return abaixoMinimo; }
    public void setAbaixoMinimo(boolean abaixoMinimo) { this.abaixoMinimo = abaixoMinimo; }
}
