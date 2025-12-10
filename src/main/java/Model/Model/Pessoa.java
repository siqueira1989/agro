/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Model;


public class Pessoa {
    protected int idPessoa;
    protected String nomePessoa;
    protected String usuarioPessoa; // Removido static
    protected String senhaPessoa;
    protected String nivelPessoa;
    protected boolean situacaoPessoa; // Removido static
    protected String emailPessoa;
    protected String telefonePessoa;
    private String Cep;
    private int numero;
    private String complemento;
	public int getIdPessoa() {
		return idPessoa;
	}
	public void setIdPessoa(int idPessoa) {
		this.idPessoa = idPessoa;
	}
	public String getNomePessoa() {
		return nomePessoa;
	}
	public void setNomePessoa(String nomePessoa) {
		this.nomePessoa = nomePessoa;
	}
	public String getUsuarioPessoa() {
		return usuarioPessoa;
	}
	public void setUsuarioPessoa(String usuarioPessoa) {
		this.usuarioPessoa = usuarioPessoa;
	}
	public String getSenhaPessoa() {
		return senhaPessoa;
	}
	public void setSenhaPessoa(String senhaPessoa) {
		this.senhaPessoa = senhaPessoa;
	}
	public String getNivelPessoa() {
		return nivelPessoa;
	}
	public void setNivelPessoa(String nivelPessoa) {
		this.nivelPessoa = nivelPessoa;
	}
	public boolean isSituacaoPessoa() {
		return situacaoPessoa;
	}
	public void setSituacaoPessoa(boolean situacaoPessoa) {
		this.situacaoPessoa = situacaoPessoa;
	}
	public String getEmailPessoa() {
		return emailPessoa;
	}
	public void setEmailPessoa(String emailPessoa) {
		this.emailPessoa = emailPessoa;
	}
	public String getTelefonePessoa() {
		return telefonePessoa;
	}
	public void setTelefonePessoa(String telefonePessoa) {
		this.telefonePessoa = telefonePessoa;
	}
          public String getCep() {
        return Cep;
    }

    public void setCep(String Cep) {
        this.Cep = Cep;
    }
	
	public int getNumero() {
		return numero;
	}
	public void setNumero(int numero) {
		this.numero = numero;
	}
	public String getComplemento() {
		return complemento;
	}
	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}
	public Pessoa(int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, String nivelPessoa,
			boolean situacaoPessoa, String emailPessoa, String telefonePessoa, String cep, int numero,
			String complemento) {
		super();
		this.idPessoa = idPessoa;
		this.nomePessoa = nomePessoa;
		this.usuarioPessoa = usuarioPessoa;
		this.senhaPessoa = senhaPessoa;
		this.nivelPessoa = nivelPessoa;
		this.situacaoPessoa = situacaoPessoa;
		this.emailPessoa = emailPessoa;
		this.telefonePessoa = telefonePessoa;
		this.Cep = cep;
		this.numero = numero;
		this.complemento = complemento;
	}
	public Pessoa() {}

  
  }

