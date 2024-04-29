package com.app.preorder.service.email;

public interface EmailService {
    void sendMail(String to, String sub, String text);
}
