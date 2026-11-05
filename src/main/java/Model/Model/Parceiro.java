
package Model.Model;

public class Parceiro extends PessoaCnpj {
	private String siteparceiro;
	/** Classificação de negócio: PARCEIRO | FORNECEDOR | INSUMO (ver TipoParceiro). */
	private String tipoParceiro;


    public String getSiteparceiro() {
        return siteparceiro;
    }

    public void setSiteparceiro(String siteparceiro) {
        this.siteparceiro = siteparceiro;
    }

    private String logradouro;
    private String bairro;
    private String cidade;
    private String uf;

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getTipoParceiro() {
        return tipoParceiro;
    }

    public void setTipoParceiro(String tipoParceiro) {
        this.tipoParceiro = tipoParceiro;
    }
  
	public Parceiro(
                int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa, boolean situacaoPessoa, String emailPessoa, String telefonePessoa, String cep, int numero, String complemento, String cnpjPessoaCnpj, String razaoSocialPessoaCnpj, String inscricaoEstadualPessoaCnpj, String siteparceiro) {
		super( idPessoa, nomePessoa, usuarioPessoa,  senhaPessoa,  nivelPessoa, situacaoPessoa, 
        		emailPessoa, telefonePessoa,  cep ,numero, complemento, cnpjPessoaCnpj,
				razaoSocialPessoaCnpj, inscricaoEstadualPessoaCnpj);
		this.siteparceiro = siteparceiro;
	}    

}
