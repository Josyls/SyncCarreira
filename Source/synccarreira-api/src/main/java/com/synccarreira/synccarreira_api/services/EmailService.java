package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.services.exceptions.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
public class EmailService {

    public static final String FIRST_ACCESS_TEMPLATE = "template-thymeleaf.html";

    public static final String JOURNEY_DOUBT_TEMPLATE = "journey-doubt-template.html";
    public static final String FEEDBACK_TEMPLATE = "feedback-template.html";

    private final JavaMailSender emailSender;

    private final SpringTemplateEngine thymeleafTemplateEngine;

    public EmailService(final JavaMailSender emailSender, final SpringTemplateEngine thymeleafTemplateEngine) {
        this.emailSender = emailSender;
        this.thymeleafTemplateEngine = thymeleafTemplateEngine;
    }

    public void sendHtmlMessage(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        emailSender.send(message);
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
        } catch (MessagingException e) {
            throw new EmailException("Falha no envio do e-mail.");
        }
    }
}
