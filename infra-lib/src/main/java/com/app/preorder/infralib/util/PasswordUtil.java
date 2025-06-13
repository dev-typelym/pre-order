package com.app.preorder.infralib.util;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;


public class PasswordUtil {

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }

    public String encodePassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }
}