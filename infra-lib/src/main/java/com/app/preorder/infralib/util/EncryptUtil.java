package com.app.preorder.infralib.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptUtil {

    private final AesBytesEncryptor encryptor;

    public EncryptUtil(@Value("${aes.key}") String key,
                       @Value("${aes.salt}") String salt) {
        this.encryptor = new AesBytesEncryptor(key, salt);
    }

    // 암호화
    public String encrypt(String str) {
        byte[] encrypt = encryptor.encrypt(str.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypt);
    }

    // 복호화
    public String decrypt(String encrypted) {
        byte[] decryptBytes = Base64.getDecoder().decode(encrypted);
        byte[] decrypt = encryptor.decrypt(decryptBytes);
        return new String(decrypt, StandardCharsets.UTF_8);
    }
}
