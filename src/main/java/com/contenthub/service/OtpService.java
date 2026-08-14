package com.contenthub.service;

import com.contenthub.entity.OtpToken;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.repository.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.expiry-minutes}")
    private int expiryMinutes;

    public OtpService(OtpTokenRepository otpTokenRepository, EmailService emailService) {
        this.otpTokenRepository = otpTokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void generateAndSend(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));

        OtpToken token = new OtpToken();
        token.setEmail(email.trim().toLowerCase());
        token.setOtpCode(otp);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        token.setUsed(false);
        otpTokenRepository.save(token);

        emailService.sendOtpEmail(email, otp, expiryMinutes);
    }

    @Transactional
    public void verify(String email, String otp) {
        OtpToken token = otpTokenRepository
                .findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email.trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("No OTP found for this email. Please request a new one."));

        if (token.isUsed()) {
            throw new BadRequestException("This OTP has already been used.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This OTP has expired. Please request a new one.");
        }
        if (!token.getOtpCode().equals(otp.trim())) {
            throw new BadRequestException("Incorrect OTP. Please try again.");
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    // Runs every hour, deletes OTPs older than their expiry to keep the table small
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void cleanupExpired() {
        otpTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));
    }
}
