package com.ticketsystem.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTicketEmail(String to, String subject, Map<String, Object> variables, String pdfPath) {
        try {
            log.info("=== Bắt đầu gửi email ===");
            log.info("From: {}", fromEmail);
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("PDF path: {}", pdfPath);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Process Thymeleaf template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("ticket-email", context);
            helper.setText(htmlContent, true);

            // Attach PDF
            if (pdfPath != null) {
                File pdfFile = new File(pdfPath);
                if (pdfFile.exists()) {
                    FileSystemResource file = new FileSystemResource(pdfFile);
                    helper.addAttachment("ticket.pdf", file);
                    log.info("PDF file attached: {} (size: {} bytes)", pdfPath, pdfFile.length());
                } else {
                    log.warn("PDF file not found: {}", pdfPath);
                }
            }

            mailSender.send(message);
            log.info("=== Email gửi thành công tới {} ===", to);
        } catch (MessagingException e) {
            log.error("MessagingException khi gửi email: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception khi gửi email: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }
}
