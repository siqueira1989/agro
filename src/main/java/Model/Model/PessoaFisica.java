
package Model.Model;

import java.time.LocalDate;


public class PessoaFisica extends Pessoa{
    private String cpfPf;
    private LocalDate dataNascimentoPf;


    public String getCpfPf() {
        return cpfPf;
    }

    public void setCpfPf(String cpfPf) {
        this.cpfPf = cpfPf;
    }


    public LocalDate getDataNascimentoPf() {
        return dataNascimentoPf;
    }

    public void setDataNascimentoPf(LocalDate dataNascimentoPf) {
        this.dataNascimentoPf = dataNascimentoPf;
    }

  
   

    public PessoaFisica(
    		int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa,
			boolean situacaoPessoa, String emailPessoa, String telefonePessoa, String cep, int numero,
			String complemento, String cpfPf,LocalDate dataNascimentoPf 
    		) {
        super(idPessoa, nomePessoa, usuarioPessoa,  senhaPessoa,  nivelPessoa, situacaoPessoa, 
        		emailPessoa, telefonePessoa,  cep ,numero, complemento);
        this.cpfPf = cpfPf;
        this.dataNascimentoPf = dataNascimentoPf;
        
    }
    
    
}
