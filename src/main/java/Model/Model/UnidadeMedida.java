package Model.Model;

/**
 * Unidades de medida para insumos, agrupadas por grandeza:
 *   - MASSA:      G (grama), KG (quilograma), T (tonelada)
 *   - CAPACIDADE: ML (mililitro), L (litro)
 *   - UNIDADE:    UN (unidade)
 */
public enum UnidadeMedida {

    G  ("Grama",       "MASSA"),
    KG ("Quilograma",  "MASSA"),
    T  ("Tonelada",    "MASSA"),
    ML ("Mililitro",   "CAPACIDADE"),
    L  ("Litro",       "CAPACIDADE"),
    UN ("Unidade",     "UNIDADE");

    private final String rotulo;
    private final String grandeza;

    UnidadeMedida(String rotulo, String grandeza) {
        this.rotulo = rotulo;
        this.grandeza = grandeza;
    }

    public String getRotulo()   { return rotulo; }
    public String getGrandeza() { return grandeza; }

    public static boolean isValido(String nome) {
        if (nome == null) return false;
        for (UnidadeMedida u : values()) if (u.name().equalsIgnoreCase(nome)) return true;
        return false;
    }

    /** Verifica se a unidade pertence à grandeza informada (MASSA/CAPACIDADE/UNIDADE). */
    public static boolean pertence(String unidade, String grandeza) {
        if (unidade == null || grandeza == null) return false;
        for (UnidadeMedida u : values()) {
            if (u.name().equalsIgnoreCase(unidade)) {
                return u.grandeza.equalsIgnoreCase(grandeza);
            }
        }
        return false;
    }
}
