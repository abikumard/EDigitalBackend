package com.contenthub.service;

import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User signup(String rawIdentifier, String password, String confirmPassword) {
        String identifier = rawIdentifier == null ? "" : rawIdentifier.trim();
        if (identifier.isEmpty()) {
            throw new BadRequestException("Please enter your email or mobile number.");
        }
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Password and Confirm Password do not match.");
        }

        User user = new User();

        if (isEmail(identifier)) {
            String email = identifier.toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new BadRequestException("An account with this email already exists. Please log in instead.");
            }
            user.setEmail(email);
        } else {
            String mobile = normalizeMobile(identifier);
            if (mobile == null) {
                throw new BadRequestException("Please enter a valid email address or a 10-digit mobile number.");
            }
            if (userRepository.existsByMobile(mobile)) {
                throw new BadRequestException("An account with this mobile number already exists. Please log in instead.");
            }
            user.setMobile(mobile);
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    @Transactional
    public User authenticate(String rawIdentifier, String password) {
        String identifier = rawIdentifier == null ? "" : rawIdentifier.trim();
        User user = null;

        if (isEmail(identifier)) {
            user = userRepository.findByEmailIgnoreCase(identifier.toLowerCase()).orElse(null);
        } else {
            String mobile = normalizeMobile(identifier);
            if (mobile != null) {
                user = userRepository.findByMobile(mobile).orElse(null);
            }
        }

        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect email/mobile or password.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private boolean isEmail(String identifier) {
        return identifier.indexOf('@') >= 0;
    }

    // Strips spaces/dashes and an optional +91 / 0 prefix, then checks it's a valid 10-digit Indian mobile number.
    // Returns the clean 10-digit number, or null if it doesn't look like a valid mobile number.
    private String normalizeMobile(String raw) {
        StringBuilder digitsOnly = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) {
                digitsOnly.append(c);
            }
        }

        String cleaned = digitsOnly.toString();
        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.length() != 10) {
            return null;
        }
        char firstDigit = cleaned.charAt(0);
        if (firstDigit < '6' || firstDigit > '9') {
            return null;
        }
        return cleaned;
    }
}
