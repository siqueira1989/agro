package Model.Model;

public class PessoaCnpj extends Pessoa {

    protected String cnpjPessoaCnpj;
    protected String razaoSocialPessoaCnpj;
    protected String inscricaoEstadualPessoaCnpj;

    public PessoaCnpj(int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa,
			boolean situacaoPessoa, String emailPessoa, String telefonePessoa, Endereco endereco, int numero,
			String complemento, String cnpjPessoaCnpj, String razaoSocialPessoaCnpj, String inscricaoEstadualPessoaCnpj) {
        super(idPessoa, nomePessoa, usuarioPessoa,  senhaPessoa,  nivelPessoa, situacaoPessoa, 
        		emailPessoa, telefonePessoa,  endereco,numero, complemento);
        this.cnpjPessoaCnpj = cnpjPessoaCnpj;
        this.razaoSocialPessoaCnpj = razaoSocialPessoaCnpj;
        this.inscricaoEstadualPessoaCnpj = inscricaoEstadualPessoaCnpj;
    }



    // Getters e Setters
    public String getCnpjPessoaCnpj() {
        return cnpjPessoaCnpj;
    }

    public void setCnpjPessoaCnpj(String cnpjPessoaCnpj) {
        this.cnpjPessoaCnpj = cnpjPessoaCnpj;
    }

    public String getRazaoSocialPessoaCnpj() {
        return razaoSocialPessoaCnpj;
    }

    public void setRazaoSocialPessoaCnpj(String razaoSocialPessoaCnpj) {
        this.razaoSocialPessoaCnpj = razaoSocialPessoaCnpj;
    }

    public String getInscricaoEstadualPessoaCnpj() {
        return inscricaoEstadualPessoaCnpj;
    }

    public void setInscricaoEstadualPessoaCnpj(String inscricaoEstadualPessoaCnpj) {
        this.inscricaoEstadualPessoaCnpj = inscricaoEstadualPessoaCnpj;
    }
}
