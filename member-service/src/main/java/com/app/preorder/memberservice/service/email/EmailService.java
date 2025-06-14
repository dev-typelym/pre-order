package com.app.preorder.memberservice.service.email;

public interface EmailService {
    void sendSignupVerificationMail(String to, String verificationLink);
}
