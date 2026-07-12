package Model.Model;

/**
 * Classificação de negócio de um parceiro (independente do nivelpessoa/login):
 *   - PARCEIRO:   compra a produção agrícola
 *   - FORNECEDOR: vende produto indiretamente da produção (caixas, gasolina, etc.)
 *   - INSUMO:     vende produto usado direto na produção (defensivos e adubos)
 */
public enum TipoParceiro {

    PARCEIRO   ("Parceiro",   "Compra a produção agrícola"),
    FORNECEDOR ("Fornecedor", "Vende produto indiretamente (caixas, gasolina)"),
    INSUMO     ("Insumo",     "Vende defensivos e adubos usados na produção");

    private final String rotulo;
    private final String descricao;

    TipoParceiro(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo()    { return rotulo; }
    public String getDescricao() { return descricao; }

    public static boolean isValido(String nome) {
        if (nome == null) return false;
        for (TipoParceiro t : values()) if (t.name().equalsIgnoreCase(nome)) return true;
        return false;
    }

    /** Normaliza a entrada (aceita rótulo antigo em minúsculas) para o name() do enum. */
    public static String normalizar(String valor) {
        if (valor == null) return PARCEIRO.name();
        String v = valor.trim().toUpperCase();
        return isValido(v) ? v : PARCEIRO.name();
    }
}
