package Model.Dao;

/**
 * Sinaliza tentativa de saída maior que o saldo disponível — o estoque não
 * pode ficar negativo. Tratada pelo controller como erro de negócio (400).
 */
public class EstoqueInsuficienteException extends Exception {
    public EstoqueInsuficienteException(String msg) {
        super(msg);
    }
}
