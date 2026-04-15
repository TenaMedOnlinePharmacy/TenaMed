package com.TenaMed.email.service;

import com.TenaMed.email.dto.EmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender javaMailSender;

    public SmtpEmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    @Async
    public void sendEmail(EmailRequest request) {
        if (request == null) {
            log.error("Email request is required");
            return;
        }

        try {
            if (request.isHtml()) {
                sendHtml(request);
            } else {
                sendPlainText(request);
            }
        } catch (Exception ex) {
            log.error("Failed to send email to {}", request.getTo(), ex);
        }
    }

    private void sendPlainText(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());
        javaMailSender.send(message);
    }

    private void sendHtml(EmailRequest request) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setTo(request.getTo());
        helper.setSubject(request.getSubject());
        helper.setText(request.getBody(), true);
        javaMailSender.send(mimeMessage);
    }
}
