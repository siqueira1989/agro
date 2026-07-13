package Model.Model;

/**
 * Categoria do insumo agrícola usado direto na produção.
 * Extensível: futuras categorias de controle (ex.: EMBALAGEM, COMBUSTIVEL) podem
 * ser adicionadas aqui quando fornecedores indiretos ganharem controle próprio.
 */
public enum CategoriaInsumo {

    DEFENSIVO ("Defensivo"),
    ADUBO     ("Adubo");

    private final String rotulo;

    CategoriaInsumo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() { return rotulo; }

    public static boolean isValido(String nome) {
        if (nome == null) return false;
        for (CategoriaInsumo c : values()) if (c.name().equalsIgnoreCase(nome)) return true;
        return false;
    }
}
