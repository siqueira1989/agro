
package Model.Model;
public class Endereco {
        private int idendereco;
	private String Endereco; 
	private  String Cep;
        private String Bairro;
	private String Cidade;
	private String Estado;
	private String Pais;
     

    public int getIdendereco() {
        return idendereco;
    }

    public void setIdendereco(int idendereco) {
        this.idendereco = idendereco;
    }

    public String getEndereco() {
        return Endereco;
    }

    public void setEndereco(String Endereco) {
        this.Endereco = Endereco;
    }

    public String getCep() {
        return Cep;
    }

    public void setCep(String Cep) {
        this.Cep = Cep;
    }

    public String getBairro() {
        return Bairro;
    }

    public void setBairro(String Bairro) {
        this.Bairro = Bairro;
    }

    public String getCidade() {
        return Cidade;
    }

    public void setCidade(String Cidade) {
        this.Cidade = Cidade;
    }

    public String getEstado() {
        return Estado;
    }

    public void setEstado(String Estado) {
        this.Estado = Estado;
    }

    public String getPais() {
        return Pais;
    }

    public void setPais(String Pais) {
        this.Pais = Pais;
    }

	public Endereco(int idendereco, String endereco, String cep, String bairro, String cidade, String estado,
			String pais) {
		super();
		this.idendereco = idendereco;
		Endereco = endereco;
		Cep = cep;
		Bairro = bairro;
		Cidade = cidade;
		Estado = estado;
		Pais = pais;
	}

	public Endereco() {
		
	}
    
}

