package com.app.preorder.memberservice.service.email;

public interface EmailService {
    void sendMail(String to, String sub, String text);
}
