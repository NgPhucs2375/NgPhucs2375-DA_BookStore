package com.example.bookstore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
        JavaMailSender mailSender,
        @Value("${app.mail.from:${spring.mail.username:no-reply@bookstore.local}}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtpEmail(String toEmail, String otp, long expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromAddress);
        message.setSubject("[BookStore] Ma OTP xac thuc dang ky");
        message.setText(buildOtpBody(otp, expireMinutes));
        mailSender.send(message);
    }

    private String buildOtpBody(String otp, long expireMinutes) {
        return "Xin chao,\n\n"
            + "Ma OTP xac thuc dang ky BookStore cua ban la: " + otp + "\n"
            + "Ma co hieu luc trong " + expireMinutes + " phut.\n\n"
            + "Neu ban khong yeu cau thao tac nay, vui long bo qua email nay.\n\n"
            + "BookStore Team";
    }
}
