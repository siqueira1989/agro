package Util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitário de envio de e-mail via SMTP (Jakarta Mail).
 *
 * Configure SMTP_HOST, SMTP_USER e SMTP_PASS com as credenciais reais.
 * Para testes locais, use Mailtrap (https://mailtrap.io) ou Gmail com App Password.
 *
 * Gmail: SMTP_HOST=smtp.gmail.com, PORT=587, TLS=true
 * Mailtrap: SMTP_HOST=smtp.mailtrap.io, PORT=587
 */
public class EmailUtil {

    private static final Logger LOG = Logger.getLogger(EmailUtil.class.getName());

    // ── Configurações SMTP ─────────────────────────────────────────────────
    // Lidas de src/main/resources/email.properties (não commitar no Git)
    // Fallback: variável de sistema Java (-Dmail.smtp.user=...)
    private static final String SMTP_HOST;
    private static final String SMTP_PORT;
    private static final String SMTP_USER;
    private static final String SMTP_PASS;
    private static final String FROM_NAME = "Agro Tech One";

    // E-mails de admin/gerente que recebem CC em todos os envios
    private static final List<InternetAddress> ADMIN_CC;

    static {
        Properties file = new Properties();
        try (var in = EmailUtil.class.getClassLoader()
                         .getResourceAsStream("email.properties")) {
            if (in != null) file.load(in);
        } catch (Exception e) {
            LOG.warning("email.properties não encontrado — usando valores padrão.");
        }
        SMTP_HOST = file.getProperty("mail.smtp.host",
                    System.getProperty("mail.smtp.host", "smtp.gmail.com"));
        SMTP_PORT = file.getProperty("mail.smtp.port",
                    System.getProperty("mail.smtp.port", "587"));
        SMTP_USER = file.getProperty("mail.smtp.user",
                    System.getProperty("mail.smtp.user", ""));
        SMTP_PASS = file.getProperty("mail.smtp.pass",
                    System.getProperty("mail.smtp.pass", ""));

        // Carrega lista de admin/gerente (separados por vírgula)
        String adminRaw = file.getProperty("mail.admin.emails",
                          System.getProperty("mail.admin.emails", ""));
        List<InternetAddress> cc = new java.util.ArrayList<>();
        for (String addr : adminRaw.split(",")) {
            addr = addr.trim();
            if (!addr.isEmpty()) {
                try { cc.add(new InternetAddress(addr)); }
                catch (Exception ignore) { LOG.warning("E-mail admin inválido: " + addr); }
            }
        }
        ADMIN_CC = java.util.Collections.unmodifiableList(cc);
    }

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });
    }

    /**
     * Envia comprovante de marcação de ponto ao funcionário.
     *
     * @param destinatario  E-mail do funcionário
     * @param nomeFuncionario Nome do funcionário
     * @param campo         Nome da marcação (Entrada 1, Saída 1, Entrada 2, Saída 2)
     * @param hora          Hora registrada
     * @param data          Data do registro
     */
    /**
     * Envia comprovante de ponto ao funcionário (TO) e ao admin/gerente (CC).
     * Ambos recebem o mesmo e-mail — o corpo identifica claramente o funcionário,
     * a data e o tipo de marcação (Entrada 1 / Saída 1 / Entrada 2 / Saída 2).
     */
    public static void enviarComprovantePonto(String destinatario, String nomeFuncionario,
                                              String campo, LocalTime hora, LocalDate data) {
        if (destinatario == null || destinatario.isBlank()) {
            // funcionário sem e-mail: envia só para o admin se houver
            if (ADMIN_CC.isEmpty()) {
                LOG.warning("Ponto não notificado: e-mail do funcionário vazio e sem admin configurado.");
                return;
            }
            enviarSomenteAdmin(nomeFuncionario, campo, hora, data);
            return;
        }
        try {
            String horaFmt = hora.format(DateTimeFormatter.ofPattern("HH:mm"));
            String dataFmt = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String diaSem  = data.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL, new java.util.Locale("pt", "BR"));
            // Capitaliza primeira letra
            diaSem = diaSem.substring(0, 1).toUpperCase() + diaSem.substring(1);

            Session session = buildSession();
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            adicionarCC(msg);   // gerente/admin em cópia automática
            // Assunto inclui nome do funcionário — admin identifica de quem é
            msg.setSubject("[Ponto] " + nomeFuncionario + " — " + campo + " às " + horaFmt);
            msg.setContent(buildHtmlPonto(nomeFuncionario, campo, horaFmt, dataFmt, diaSem),
                    "text/html; charset=utf-8");
            Transport.send(msg);
            LOG.info(() -> "Comprovante enviado → TO:" + destinatario
                    + (ADMIN_CC.isEmpty() ? "" : " CC:" + ADMIN_CC));
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOG.log(Level.WARNING, "Falha ao enviar e-mail de ponto: " + e.getMessage(), e);
        }
    }

    /** Envia aviso de ponto apenas para o admin quando o funcionário não tem e-mail. */
    private static void enviarSomenteAdmin(String nomeFuncionario, String campo,
                                           LocalTime hora, LocalDate data) {
        try {
            String horaFmt = hora.format(DateTimeFormatter.ofPattern("HH:mm"));
            String dataFmt = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String diaSem  = data.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL, new java.util.Locale("pt", "BR"));
            diaSem = diaSem.substring(0, 1).toUpperCase() + diaSem.substring(1);

            Session session = buildSession();
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
            msg.setRecipients(Message.RecipientType.TO,
                    ADMIN_CC.toArray(new InternetAddress[0]));
            msg.setSubject("[Ponto] " + nomeFuncionario + " — " + campo + " às " + horaFmt);
            msg.setContent(buildHtmlPonto(nomeFuncionario, campo, horaFmt, dataFmt, diaSem),
                    "text/html; charset=utf-8");
            Transport.send(msg);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Falha ao enviar ponto para admin: " + e.getMessage(), e);
        }
    }

    /**
     * Envia resumo do fechamento de folha ao funcionário.
     */
    public static void enviarResumoFolha(String destinatario, String nomeFuncionario,
                                          String periodo, String salarioBruto,
                                          String salarioLiquido, String totalExtras,
                                          String totalVales, String descontoFaltas) {
        if (destinatario == null || destinatario.isBlank()) return;
        try {
            Session session = buildSession();
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            adicionarCC(msg);   // gerente/admin em cópia
            // Assunto inclui nome do funcionário para o admin identificar
            msg.setSubject("[Folha] " + nomeFuncionario + " — " + periodo);
            msg.setContent(buildHtmlFolha(nomeFuncionario, periodo, salarioBruto,
                    salarioLiquido, totalExtras, totalVales, descontoFaltas),
                    "text/html; charset=utf-8");
            Transport.send(msg);
            LOG.info("Resumo de folha enviado → TO:" + destinatario
                    + (ADMIN_CC.isEmpty() ? "" : " CC:" + ADMIN_CC));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Falha ao enviar resumo de folha: " + e.getMessage(), e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Adiciona todos os e-mails de admin/gerente como CC na mensagem. */
    private static void adicionarCC(MimeMessage msg) throws MessagingException {
        if (ADMIN_CC.isEmpty()) return;
        msg.setRecipients(Message.RecipientType.CC,
                ADMIN_CC.toArray(new InternetAddress[0]));
    }

    // ── templates HTML ────────────────────────────────────────────────────

    private static String buildHtmlPonto(String nome, String campo,
                                          String hora, String data, String diaSemana) {
        boolean entrada = campo.toLowerCase().contains("entrada");
        String corMarcacao = entrada ? "#28a745" : "#dc3545";
        String icone       = entrada ? "&#x1F7E2;" : "&#x1F534;"; // 🟢 🔴

        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;"
                + "background:#f0f4f8;padding:24px;margin:0'>"
                + "<div style='max-width:500px;margin:auto;background:#fff;border-radius:10px;"
                + "box-shadow:0 4px 12px rgba(0,0,0,.12);overflow:hidden'>"

                // cabeçalho verde
                + "<div style='background:#28a745;color:#fff;padding:22px 24px'>"
                + "<h2 style='margin:0;font-size:20px'>&#127807; Agro Tech One</h2>"
                + "<p style='margin:4px 0 0;opacity:.85;font-size:13px'>Registro de Ponto Eletrônico</p>"
                + "</div>"

                // faixa colorida do tipo de marcação
                + "<div style='background:" + corMarcacao + ";color:#fff;padding:14px 24px;"
                + "font-size:18px;font-weight:700;letter-spacing:.5px'>"
                + icone + " &nbsp;" + campo.toUpperCase()
                + "</div>"

                // corpo
                + "<div style='padding:24px'>"
                + "<p style='font-size:15px;margin:0 0 16px'>O ponto do funcionário abaixo foi "
                + "registrado automaticamente pelo sistema:</p>"

                + "<table style='width:100%;border-collapse:collapse;font-size:14px'>"
                + rowDestaque("Funcionário", nome)
                + row("Tipo de Marcação", campo)
                + row("Data",             diaSemana + ", " + data)
                + row("Horário",          hora)
                + "</table>"

                // aviso para o funcionário
                + "<div style='margin-top:20px;background:#f8f9fa;border-left:4px solid #28a745;"
                + "padding:12px 16px;border-radius:4px;font-size:13px;color:#495057'>"
                + "<strong>Funcionário:</strong> se o horário acima estiver incorreto, "
                + "informe o RH para ajuste.<br>"
                + "<strong>Administração:</strong> este e-mail é uma cópia automática para controle."
                + "</div></div>"

                // rodapé
                + "<div style='background:#f8f9fa;padding:12px 24px;text-align:center;"
                + "font-size:12px;color:#6c757d;border-top:1px solid #dee2e6'>"
                + "Agro Tech One &mdash; Sistema de Gestão Agrícola &bull; E-mail automático"
                + "</div></div></body></html>";
    }

    private static String buildHtmlFolha(String nome, String periodo, String bruto,
                                          String liquido, String extras,
                                          String vales, String descFaltas) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;"
                + "background:#f0f4f8;padding:24px;margin:0'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:10px;"
                + "box-shadow:0 4px 12px rgba(0,0,0,.12);overflow:hidden'>"

                // cabeçalho
                + "<div style='background:#28a745;color:#fff;padding:22px 24px'>"
                + "<h2 style='margin:0;font-size:20px'>&#127807; Agro Tech One</h2>"
                + "<p style='margin:4px 0 0;opacity:.85;font-size:13px'>Fechamento de Folha &mdash; " + periodo + "</p>"
                + "</div>"

                // faixa com nome do funcionário
                + "<div style='background:#155724;color:#fff;padding:12px 24px;"
                + "font-size:16px;font-weight:700'>"
                + "&#128100; &nbsp;" + nome
                + "</div>"

                // corpo
                + "<div style='padding:24px'>"
                + "<p style='font-size:15px;margin:0 0 16px;color:#333'>"
                + "Segue o resumo financeiro do período <strong>" + periodo + "</strong>:</p>"

                + "<table style='width:100%;border-collapse:collapse;font-size:14px'>"
                + row("Salário Base",        "R$ " + bruto)
                + row("+ Horas Extras",      "R$ " + extras)
                + row("&minus; Desc. Faltas","R$ " + descFaltas)
                + row("Salário Bruto",       "R$ " + bruto)
                + row("&minus; Vales",       "R$ " + vales)
                + "</table>"

                // destaque do líquido
                + "<div style='margin-top:16px;background:#d4edda;border:1px solid #c3e6cb;"
                + "border-radius:6px;padding:16px 20px;display:flex;"
                + "justify-content:space-between;align-items:center'>"
                + "<span style='font-size:15px;color:#155724;font-weight:600'>Salário Líquido</span>"
                + "<span style='font-size:22px;font-weight:700;color:#155724'>R$ " + liquido + "</span>"
                + "</div>"

                // aviso duplo
                + "<div style='margin-top:16px;background:#f8f9fa;border-left:4px solid #28a745;"
                + "padding:12px 16px;border-radius:4px;font-size:13px;color:#495057'>"
                + "<strong>Funcionário:</strong> em caso de dúvidas sobre os valores, procure o RH.<br>"
                + "<strong>Administração:</strong> cópia automática para controle de folha."
                + "</div></div>"

                // rodapé
                + "<div style='background:#f8f9fa;padding:12px 24px;text-align:center;"
                + "font-size:12px;color:#6c757d;border-top:1px solid #dee2e6'>"
                + "Agro Tech One &mdash; Sistema de Gestão Agrícola &bull; E-mail automático"
                + "</div></div></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr>"
                + "<td style='padding:9px 12px;border-bottom:1px solid #dee2e6;color:#6c757d;width:45%'>" + label + "</td>"
                + "<td style='padding:9px 12px;border-bottom:1px solid #dee2e6;font-weight:600;color:#212529'>" + value + "</td>"
                + "</tr>";
    }

    private static String rowDestaque(String label, String value) {
        return "<tr style='background:#f8fff9'>"
                + "<td style='padding:10px 12px;border-bottom:2px solid #28a745;color:#155724;font-weight:600;width:45%'>" + label + "</td>"
                + "<td style='padding:10px 12px;border-bottom:2px solid #28a745;font-weight:700;color:#155724;font-size:15px'>" + value + "</td>"
                + "</tr>";
    }
}
