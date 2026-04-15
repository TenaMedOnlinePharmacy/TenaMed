package com.TenaMed.email.service;

import com.TenaMed.email.dto.EmailRequest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTests {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    @Test
    void shouldSendPlainTextEmailWhenIsHtmlFalse() {
        EmailRequest request = new EmailRequest("doctor@example.com", "Subject", "Plain body", false);

        smtpEmailService.sendEmail(request);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        verify(javaMailSender, never()).createMimeMessage();

        SimpleMailMessage sent = captor.getValue();
        assertEquals("doctor@example.com", sent.getTo()[0]);
        assertEquals("Subject", sent.getSubject());
        assertEquals("Plain body", sent.getText());
    }

    @Test
    void shouldSendHtmlEmailWhenIsHtmlTrue() {
        EmailRequest request = new EmailRequest("doctor@example.com", "Invite", "<html>Body</html>", true);
        MimeMessage mimeMessage = org.mockito.Mockito.mock(MimeMessage.class);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailService.sendEmail(request);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldNotCallMailSenderWhenRequestIsNull() {
        smtpEmailService.sendEmail(null);

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void shouldNotThrowWhenPlainTextSendFails() {
        EmailRequest request = new EmailRequest("doctor@example.com", "Subject", "Body", false);
        doThrow(new RuntimeException("smtp error")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> smtpEmailService.sendEmail(request));
    }

    @Test
    void shouldNotThrowWhenHtmlMessageCreationFails() {
        EmailRequest request = new EmailRequest("doctor@example.com", "Subject", "<html>Body</html>", true);
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("mime error"));

        assertDoesNotThrow(() -> smtpEmailService.sendEmail(request));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}
