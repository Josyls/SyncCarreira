package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.services.exceptions.EmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTests {

    @InjectMocks
    private EmailService service;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private SpringTemplateEngine thymeleafTemplateEngine;

    private String validEmail, invalidEmail, subject, htmlBody, templateName, fromAddress, fromName;
    private Map<String, Object> templateModel;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        fromAddress = "contato@synccarreira.com.br";
        fromName = "SyncCarreira";
        ReflectionTestUtils.setField(service, "fromAddress", fromAddress);
        ReflectionTestUtils.setField(service, "fromName", fromName);

        validEmail = "ana.souza@gmail.com";
        invalidEmail = "Ana Souza <ana.souza@gmail.com";
        subject = "SyncCarreira - Primeiro Acesso";
        htmlBody = "<html><body><p>Olá, Ana Souza!</p></body></html>";
        templateName = "template-thymeleaf.html";

        templateModel = new HashMap<>();
        templateModel.put("recipientName", "Ana Souza");
        templateModel.put("email", validEmail);
        templateModel.put("link", "http://localhost:3000/new-password?token=abc123");

        mimeMessage = new MimeMessage((Session) null);
    }

    @Test
    void sendHtmlMessageShouldSendMimeMessageWithSenderRecipientSubjectAndHtmlAndPlainTextBodies() throws Exception {
        Mockito.when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendHtmlMessage(validEmail, subject, htmlBody);

        Mockito.verify(emailSender).send(mimeMessage);
        Assertions.assertEquals(1, mimeMessage.getFrom().length);
        InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        Assertions.assertEquals(fromAddress, from.getAddress());
        Assertions.assertEquals(fromName, from.getPersonal());
        Assertions.assertEquals(1, mimeMessage.getAllRecipients().length);
        Assertions.assertEquals(validEmail, mimeMessage.getAllRecipients()[0].toString());
        Assertions.assertEquals(subject, mimeMessage.getSubject());
        Assertions.assertEquals(htmlBody, extractHtmlBody(mimeMessage));
        Assertions.assertEquals("Olá, Ana Souza!", extractPlainTextBody(mimeMessage));
    }

    @Test
    void sendHtmlMessageShouldReturnMailSendExceptionWhenEmailSenderFails() {
        Mockito.when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
        Mockito.doThrow(MailSendException.class).when(emailSender).send(mimeMessage);

        Assertions.assertThrows(MailSendException.class, () -> {
            service.sendHtmlMessage(validEmail, subject, htmlBody);
        });
    }

    @Test
    void sendMessageUsingThymeleafTemplateShouldProcessTemplateAndSendEmailWhenDataIsValid() throws Exception {
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        Mockito.when(thymeleafTemplateEngine.process(eq(templateName), any(Context.class))).thenReturn(htmlBody);
        Mockito.when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        Assertions.assertDoesNotThrow(() -> {
            service.sendMessageUsingThymeleafTemplate(validEmail, subject, templateModel);
        });

        Mockito.verify(thymeleafTemplateEngine).process(eq(templateName), captor.capture());
        Mockito.verify(emailSender).send(mimeMessage);
        Context result = captor.getValue();

        Assertions.assertEquals(templateModel.get("recipientName"), result.getVariable("recipientName"));
        Assertions.assertEquals(templateModel.get("email"), result.getVariable("email"));
        Assertions.assertEquals(templateModel.get("link"), result.getVariable("link"));
        Assertions.assertEquals(subject, mimeMessage.getSubject());
        Assertions.assertEquals(htmlBody, extractHtmlBody(mimeMessage));
    }

    @Test
    void sendMessageUsingThymeleafTemplateShouldReturnEmailExceptionWhenRecipientIsInvalid() {
        Mockito.when(thymeleafTemplateEngine.process(eq(templateName), any(Context.class))).thenReturn(htmlBody);
        Mockito.when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        Assertions.assertThrows(EmailException.class, () -> {
            service.sendMessageUsingThymeleafTemplate(invalidEmail, subject, templateModel);
        });

        Mockito.verify(emailSender, Mockito.never()).send(any(MimeMessage.class));
    }

    @Test
    void sendMessageUsingThymeleafTemplateShouldReturnMailSendExceptionWhenEmailSenderFails() {
        Mockito.when(thymeleafTemplateEngine.process(eq(templateName), any(Context.class))).thenReturn(htmlBody);
        Mockito.when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
        Mockito.doThrow(MailSendException.class).when(emailSender).send(mimeMessage);

        Assertions.assertThrows(MailSendException.class, () -> {
            service.sendMessageUsingThymeleafTemplate(validEmail, subject, templateModel);
        });
    }

    @Test
    void toPlainTextShouldConvertHtmlIntoReadableTextKeepingLinksAndListItems() {
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\" /></head><body>"
                + "<p>Ol&aacute;, Ana &amp; Jo&#227;o!</p>\n"
                + "<p>Acesse   este link <a href=\"https://synccarreira.com.br/new-password?token=abc\">definir senha</a></p>\n"
                + "<ul>\n  <li>Nome: Ana</li>\n  <li>Turma: 3A</li>\n</ul>\n"
                + "<p>Time SyncCarreira</p></body></html>";

        String result = EmailService.toPlainText(html);

        Assertions.assertEquals("Olá, Ana & João!\n\n"
                + "Acesse este link definir senha (https://synccarreira.com.br/new-password?token=abc)\n\n"
                + "- Nome: Ana\n- Turma: 3A\n\n"
                + "Time SyncCarreira", result);
    }

    private String extractHtmlBody(MimeMessage message) throws Exception {
        return (String) extractAlternativeMultipart(message).getBodyPart(1).getContent();
    }

    private String extractPlainTextBody(MimeMessage message) throws Exception {
        return (String) extractAlternativeMultipart(message).getBodyPart(0).getContent();
    }

    private MimeMultipart extractAlternativeMultipart(MimeMessage message) throws Exception {
        MimeMultipart rootMultipart = (MimeMultipart) message.getContent();
        MimeMultipart relatedMultipart = (MimeMultipart) rootMultipart.getBodyPart(0).getContent();
        return (MimeMultipart) relatedMultipart.getBodyPart(0).getContent();
    }
}
