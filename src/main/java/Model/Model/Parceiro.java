
package Model.Model;

public class Parceiro extends PessoaCnpj {
	private String siteparceiro;

  
    public String getSiteparceiro() {
        return siteparceiro;
    }

    public void setSiteparceiro(String siteparceiro) {
        this.siteparceiro = siteparceiro;
    }
  
	public Parceiro(
                int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa, boolean situacaoPessoa, String emailPessoa, String telefonePessoa, String cep, int numero, String complemento, String cnpjPessoaCnpj, String razaoSocialPessoaCnpj, String inscricaoEstadualPessoaCnpj, String siteparceiro) {
		super( idPessoa, nomePessoa, usuarioPessoa,  senhaPessoa,  nivelPessoa, situacaoPessoa, 
        		emailPessoa, telefonePessoa,  cep ,numero, complemento, cnpjPessoaCnpj,
				razaoSocialPessoaCnpj, inscricaoEstadualPessoaCnpj);
		this.siteparceiro = siteparceiro;
	}    

}
