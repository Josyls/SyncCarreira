package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.services.exceptions.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailService {

    public static final String FIRST_ACCESS_TEMPLATE = "template-thymeleaf.html";

    public static final String JOURNEY_DOUBT_TEMPLATE = "journey-doubt-template.html";
    public static final String FEEDBACK_TEMPLATE = "feedback-template.html";

    private static final Pattern LINK_PATTERN = Pattern.compile("<a\\s[^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCK_END_PATTERN = Pattern.compile("</(p|div|ul|ol|h[1-6]|tr)>\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("(<br\\s*/?>|</li>)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("<li[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HORIZONTAL_SPACE_PATTERN = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern LINE_EDGE_SPACE_PATTERN = Pattern.compile(" *\\n *");
    private static final Pattern EXTRA_BLANK_LINES_PATTERN = Pattern.compile("\\n{3,}");

    private final JavaMailSender emailSender;

    private final SpringTemplateEngine thymeleafTemplateEngine;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    public EmailService(final JavaMailSender emailSender, final SpringTemplateEngine thymeleafTemplateEngine) {
        this.emailSender = emailSender;
        this.thymeleafTemplateEngine = thymeleafTemplateEngine;
    }

    public void sendHtmlMessage(String to, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        // Envia também a versão em texto puro (multipart/alternative); e-mails só com HTML pesam contra nos filtros anti-spam
        helper.setText(toPlainText(htmlBody), htmlBody);
        emailSender.send(message);
    }

    static String toPlainText(String htmlBody) {
        String text = LINK_PATTERN.matcher(htmlBody).replaceAll("$2 ($1)");
        text = LINE_BREAK_PATTERN.matcher(text).replaceAll("\n");
        text = BLOCK_END_PATTERN.matcher(text).replaceAll("\n\n");
        text = LIST_ITEM_PATTERN.matcher(text).replaceAll("- ");
        text = TAG_PATTERN.matcher(text).replaceAll("");
        text = HtmlUtils.htmlUnescape(text);
        text = HORIZONTAL_SPACE_PATTERN.matcher(text).replaceAll(" ");
        text = LINE_EDGE_SPACE_PATTERN.matcher(text).replaceAll("\n");
        return EXTRA_BLANK_LINES_PATTERN.matcher(text).replaceAll("\n\n").strip();
    }

    // Sem template informado, usa o template do e-mail de primeiro acesso
    public void sendMessageUsingThymeleafTemplate(String to, String subject, Map<String, Object> templateModel) {
        sendMessageUsingThymeleafTemplate(to, subject, FIRST_ACCESS_TEMPLATE, templateModel);
    }

    @Retryable(
            retryFor = { MailException.class, EmailException.class },
            noRetryFor = MailAuthenticationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendMessageUsingThymeleafTemplate(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(templateModel);
            String htmlBody = thymeleafTemplateEngine.process(templateName, thymeleafContext);
            sendHtmlMessage(to, subject, htmlBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailException("Falha no envio do e-mail.");
        }
    }
}
