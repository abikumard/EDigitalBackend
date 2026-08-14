package com.contenthub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp, int expiryMinutes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your login code");
            message.setText(
                    "Your one-time login code is: " + otp + "\n\n" +
                    "This code expires in " + expiryMinutes + " minutes.\n" +
                    "If you did not request this, you can ignore this email."
            );
            mailSender.send(message);
        } catch (Exception ex) {
            // Do not leak SMTP errors to the caller/user; log and surface a generic failure upstream
            log.error("Failed to send OTP email to {}", toEmail, ex);
            throw new RuntimeException("Could not send OTP email. Please try again shortly.");
        }
    }
}
