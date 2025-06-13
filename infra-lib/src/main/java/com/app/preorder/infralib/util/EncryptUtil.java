package com.app.preorder.infralib.util;

import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptUtil {

    private final AesBytesEncryptor encryptor;

    public EncryptUtil(AesBytesEncryptor encryptor) {
        this.encryptor = encryptor;
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
