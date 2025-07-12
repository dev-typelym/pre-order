package com.app.preorder.infralib.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class HmacHashUtil {
    private final String secretKey;

    public HmacHashUtil(@Value("${hmac.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("해시 실패", e);
        }
    }
}
