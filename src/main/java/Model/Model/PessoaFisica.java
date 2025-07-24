
package Model.Model;

import java.time.LocalDate;


public class PessoaFisica extends Pessoa{
    private String cpfPf;
    private LocalDate dataAdmissaoPf;
    private LocalDate dataNascimentoPf;


    public String getCpfPf() {
        return cpfPf;
    }

    public void setCpfPf(String cpfPf) {
        this.cpfPf = cpfPf;
    }

    public LocalDate getDataAdmissaoPf() {
        return dataAdmissaoPf;
    }

    public void setDataAdmissaoPf(LocalDate dataAdmissaoPf) {
        this.dataAdmissaoPf = dataAdmissaoPf;
    }

    public LocalDate getDataNascimentoPf() {
        return dataNascimentoPf;
    }

    public void setDataNascimentoPf(LocalDate dataNascimentoPf) {
        this.dataNascimentoPf = dataNascimentoPf;
    }

  
   

    public PessoaFisica(
    		int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa,
			boolean situacaoPessoa, String emailPessoa, String telefonePessoa, Endereco endereco, int numero,
			String complemento, String cpfPf, LocalDate dataAdmissaoPf, LocalDate dataNascimentoPf 
    		) {
        super(idPessoa, nomePessoa, usuarioPessoa,  senhaPessoa,  nivelPessoa, situacaoPessoa, 
        		emailPessoa, telefonePessoa,  endereco,numero, complemento);
        this.cpfPf = cpfPf;
        this.dataAdmissaoPf = dataAdmissaoPf;
        this.dataNascimentoPf = dataNascimentoPf;
        
    }
    
    
}
