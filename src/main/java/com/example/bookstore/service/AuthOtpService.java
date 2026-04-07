package com.example.bookstore.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;

@Service
public class AuthOtpService {

    private static final long OTP_EXPIRE_SECONDS = 300;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final MailService mailService;

    public AuthOtpService(MailService mailService) {
        this.mailService = mailService;
    }

    public void requestOtp(String email) {
        String normalizedEmail = normalize(email);
        String otp = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRE_SECONDS);
        otpStore.put(normalizedEmail, new OtpEntry(otp, expiresAt, false));

        try {
            mailService.sendOtpEmail(normalizedEmail, otp, OTP_EXPIRE_SECONDS / 60);
        } catch (Exception ex) {
            otpStore.remove(normalizedEmail);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Khong the gui OTP qua email. Vui long thu lai sau."
            );
        }
    }

    public boolean verifyOtp(String email, String otp) {
        String normalizedEmail = normalize(email);
        OtpEntry entry = otpStore.get(normalizedEmail);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            otpStore.remove(normalizedEmail);
            return false;
        }
        if (!entry.otp().equals(otp)) {
            return false;
        }

        otpStore.put(normalizedEmail, new OtpEntry(entry.otp(), entry.expiresAt(), true));
        return true;
    }

    public boolean consumeVerifiedEmail(String email) {
        String normalizedEmail = normalize(email);
        OtpEntry entry = otpStore.get(normalizedEmail);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt()) || !entry.verified()) {
            otpStore.remove(normalizedEmail);
            return false;
        }

        otpStore.remove(normalizedEmail);
        return true;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record OtpEntry(String otp, Instant expiresAt, boolean verified) {}
}
