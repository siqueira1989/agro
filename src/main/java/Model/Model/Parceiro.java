
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
