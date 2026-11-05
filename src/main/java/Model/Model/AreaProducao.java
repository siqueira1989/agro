
package Model.Model;


public class AreaProducao {
    private int idAreaProducao;
    private String PropriedadeAreaProducao;
    private String ProprietarioAreaProducao;
    private int QuantidadeTotalPlantasAreaProducao;
    private String SiglasAreaProducao;
    private String cep;
    private int numero;
    private String complemento;
    private String cpfCnpjProdutor;
    private String inscricaoEstadual;
    private String logradouro;
    private String bairro;
    private String municipio;
    private String uf;
    private boolean situacao = true;

    public int getIdAreaProducao() {
        return idAreaProducao;
    }

    public void setIdAreaProducao(int idAreaProducao) {
        this.idAreaProducao = idAreaProducao;
    }

    public String getPropriedadeAreaProducao() {
        return PropriedadeAreaProducao;
    }

    public void setPropriedadeAreaProducao(String PropriedadeAreaProducao) {
        this.PropriedadeAreaProducao = PropriedadeAreaProducao;
    }

    public String getProprietarioAreaProducao() {
        return ProprietarioAreaProducao;
    }

    public void setProprietarioAreaProducao(String ProprietarioAreaProducao) {
        this.ProprietarioAreaProducao = ProprietarioAreaProducao;
    }

    public int getQuantidadeTotalPlantasAreaProducao() {
        return QuantidadeTotalPlantasAreaProducao;
    }

    public void setQuantidadeTotalPlantasAreaProducao(int QuantidadeTotalPlantasAreaProducao) {
        this.QuantidadeTotalPlantasAreaProducao = QuantidadeTotalPlantasAreaProducao;
    }

    public String getSiglasAreaProducao() {
        return SiglasAreaProducao;
    }

    public void setSiglasAreaProducao(String SiglasAreaProducao) {
        this.SiglasAreaProducao = SiglasAreaProducao;
    }
    
    public  AreaProducao(){};

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
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

    public boolean isSituacao() {
        return situacao;
    }

    public void setSituacao(boolean situacao) {
        this.situacao = situacao;
    }

    public String getCpfCnpjProdutor() { return cpfCnpjProdutor; }
    public void setCpfCnpjProdutor(String cpfCnpjProdutor) { this.cpfCnpjProdutor = cpfCnpjProdutor; }

    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
}
