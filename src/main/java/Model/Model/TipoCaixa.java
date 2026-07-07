package Model.Model;

/**
 * Tipos de caixa de produção, por cargo do funcionário:
 *   - Colhedor:  CONTENTOR
 *   - Embalador: papelão / plástico / madeira (vários tamanhos)
 */
public enum TipoCaixa {

    CONTENTOR   ("Caixa Contentor",     "Colhedor"),
    PAPELAO_7   ("Caixa Papelão 7kg",   "Embalador"),
    PAPELAO_10  ("Caixa Papelão 10kg",  "Embalador"),
    PLASTICO_7  ("Caixa Plástico 7kg",  "Embalador"),
    PLASTICO_25 ("Caixa Plástico 25kg", "Embalador"),
    MADEIRA_7   ("Caixa Madeira 7kg",   "Embalador"),
    MADEIRA_15  ("Caixa Madeira K 15kg","Embalador");

    private final String rotulo;
    private final String cargo;

    TipoCaixa(String rotulo, String cargo) {
        this.rotulo = rotulo;
        this.cargo = cargo;
    }

    public String getRotulo() { return rotulo; }
    public String getCargo()  { return cargo; }

    public static boolean isValido(String nome) {
        if (nome == null) return false;
        for (TipoCaixa t : values()) if (t.name().equals(nome)) return true;
        return false;
    }
}
