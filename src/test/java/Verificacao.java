import Service.EncargosService;
import Service.ParametrosFolhaService;
import Util.ConnectionPool;
import Util.SenhaUtil;
import Model.Dao.PessoaDAO;
import Model.Model.Pessoa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class Verificacao {
    static int ok = 0, falhou = 0;

    static void checa(String nome, Object esperado, Object obtido) {
        boolean bom = String.valueOf(esperado).equals(String.valueOf(obtido));
        System.out.printf("  [%s] %-52s esperado=%s  obtido=%s%n",
                bom ? "OK " : "FALHA", nome, esperado, obtido);
        if (bom) ok++; else falhou++;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n=== 1. Dias uteis com feriados (P1-10) ===");
        checa("set/2026 (feriado dia 7, seg)",  21, ParametrosFolhaService.diasUteis(YearMonth.of(2026, 9)));
        checa("fev/2026 (Carnaval nao e feriado por lei)", 20, ParametrosFolhaService.diasUteis(YearMonth.of(2026, 2)));
        checa("dez/2026 (Natal, sexta)",        22, ParametrosFolhaService.diasUteis(YearMonth.of(2026, 12)));
        checa("abr/2026 (Sexta Santa 03 + Tiradentes 21)", 20, ParametrosFolhaService.diasUteis(YearMonth.of(2026, 4)));

        System.out.println("\n=== 2. Divisores da folha (P1-9, P1-11, P1-17) ===");
        BigDecimal sal = new BigDecimal("3300.00");
        checa("valorDia = salario/30",  "110.0000", ParametrosFolhaService.valorDiaFolha(sal).toString());
        checa("valorHora = salario/220", "15.0000", ParametrosFolhaService.valorHoraFolha(sal, 8).toString());
        checa("custoHoraProdutiva set/2026 (8h x 21d)", "19.64",
                ParametrosFolhaService.custoHoraProdutiva(sal, 8, YearMonth.of(2026, 9)).toString());

        System.out.println("\n=== 3. INSS progressivo (P1-9) ===");
        LocalDate comp = LocalDate.of(2026, 3, 1);
        checa("INSS sobre 5000,00",  "509.60", EncargosService.calcularInss(new BigDecimal("5000.00"), comp).toString());
        checa("INSS sobre 1500,00",  "112.50", EncargosService.calcularInss(new BigDecimal("1500.00"), comp).toString());
        checa("INSS acima do teto",  "951.63", EncargosService.calcularInss(new BigDecimal("20000.00"), comp).toString());

        System.out.println("\n=== 4. IRRF: completo x simplificado (defeito 7 da revisao) ===");
        BigDecimal bruto = new BigDecimal("5000.00");
        BigDecimal inss  = EncargosService.calcularInss(bruto, comp);
        checa("IRRF 5000 sem dependentes", "312.89",
                EncargosService.calcularIrrf(bruto, inss, 0, comp).toString());
        checa("IRRF 2000 (isento)", "0.00",
                EncargosService.calcularIrrf(new BigDecimal("2000.00"),
                        EncargosService.calcularInss(new BigDecimal("2000.00"), comp), 0, comp).toString());

        System.out.println("\n=== 5. Encargos completos ===");
        EncargosService.Encargos e = EncargosService.calcular(bruto, 0, comp);
        checa("FGTS = 8% de 5000", "400.00", e.fgts().toString());
        checa("tabela confiavel",  true, e.tabelaConfiavel());
        EncargosService.Encargos retro = EncargosService.calcular(bruto, 0, LocalDate.of(2024, 12, 1));
        checa("competencia retroativa: INSS != 0", true, retro.inss().signum() > 0);
        checa("competencia retroativa: marcada NAO confiavel", false, retro.tabelaConfiavel());

        System.out.println("\n=== 6. Hash de senha (P0-2) ===");
        String h = SenhaUtil.gerarHash("MinhaSenha123");
        checa("verifica a correta",       true,  SenhaUtil.verificar("MinhaSenha123", h));
        checa("rejeita a errada",         false, SenhaUtil.verificar("outra", h));
        checa("hashes diferem (salt)",    false, h.equals(SenhaUtil.gerarHash("MinhaSenha123")));
        checa("estaEmHash reconhece",     true,  SenhaUtil.estaEmHash(h));
        checa("nao confunde texto puro",  false, SenhaUtil.estaEmHash("pbkdf2$sha256$abc"));
        checa("hashOuVazio em branco",    "",    SenhaUtil.hashOuVazio("  "));
        checa("hashOuVazio nao re-hasheia", h,   SenhaUtil.hashOuVazio(h));

        System.out.println("\n=== 7. Login com hash no banco (P0-2 ponta a ponta) ===");
        try (java.sql.Connection c = new Util.PostgresConnection().getConnection();
             java.sql.PreparedStatement st = c.prepareStatement(
                     "UPDATE pessoa SET senhapessoa=? WHERE usuariopessoa='joao'")) {
            st.setString(1, SenhaUtil.gerarHash("segredo"));
            st.executeUpdate();
        }
        PessoaDAO dao = new PessoaDAO();
        Pessoa cred = new Pessoa();
        cred.setUsuarioPessoa("joao"); cred.setSenhaPessoa("segredo");
        checa("login com senha certa",  true,  dao.validarUsuario(cred) != null);
        cred.setSenhaPessoa("errada");
        checa("login com senha errada", false, dao.validarUsuario(cred) != null);
        cred.setUsuarioPessoa("nao_existe"); cred.setSenhaPessoa("x");
        checa("usuario inexistente",    false, dao.validarUsuario(cred) != null);

        System.out.println("\n=== 8. Pool de conexoes (P0-4) ===");
        ConnectionPool pool = ConnectionPool.getInstance();
        for (int i = 0; i < 60; i++) {
            try (java.sql.Connection c = new Util.PostgresConnection().getConnection();
                 java.sql.Statement st = c.createStatement()) { st.execute("SELECT 1"); }
        }
        checa("60 emprestimos, nada vazado", true, pool.estatisticas().startsWith("0 em uso"));
        System.out.println("      -> " + pool.estatisticas());

        System.out.println("\n=== 9. Transacao abandonada nao contamina o proximo ===");
        java.sql.Connection suja = new Util.PostgresConnection().getConnection();
        suja.setAutoCommit(false);
        try (java.sql.Statement st = suja.createStatement()) { st.execute("SELECT 1"); }
        suja.close();  // devolve ao pool sem commit
        try (java.sql.Connection c = new Util.PostgresConnection().getConnection()) {
            checa("autoCommit restaurado", true, c.getAutoCommit());
        }

        System.out.println("\n=== 10. Proxy: usar depois do close falha ===");
        java.sql.Connection fechada = new Util.PostgresConnection().getConnection();
        fechada.close();
        boolean lancou = false;
        try { fechada.createStatement(); } catch (java.sql.SQLException ex) { lancou = true; }
        checa("createStatement apos close lanca", true, lancou);
        checa("isClosed apos close", true, fechada.isClosed());

        pool.encerrar();
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("  " + ok + " verificacoes OK, " + falhou + " falharam");
        System.out.println("══════════════════════════════════════════\n");
        if (falhou > 0) System.exit(1);
    }
}
