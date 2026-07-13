package Model.Model;

import java.time.LocalDate;

public class FaltaFuncionario {

    private int       idFalta;
    private int       idFuncionario;
    private String    nomeFuncionario;
    private LocalDate dataFalta;
    private boolean   justificada;
    private String    motivo;

    public int getIdFalta()                       { return idFalta; }
    public void setIdFalta(int v)                 { this.idFalta = v; }

    public int getIdFuncionario()                 { return idFuncionario; }
    public void setIdFuncionario(int v)           { this.idFuncionario = v; }

    public String getNomeFuncionario()            { return nomeFuncionario; }
    public void setNomeFuncionario(String v)      { this.nomeFuncionario = v; }

    public LocalDate getDataFalta()               { return dataFalta; }
    public void setDataFalta(LocalDate v)         { this.dataFalta = v; }

    public boolean isJustificada()                { return justificada; }
    public void setJustificada(boolean v)         { this.justificada = v; }

    public String getMotivo()                     { return motivo; }
    public void setMotivo(String v)               { this.motivo = v; }
}
