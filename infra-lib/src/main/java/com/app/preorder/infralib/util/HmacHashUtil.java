package com.app.preorder.infralib.util;

import org.springframework.stereotype.Component;

@Component
public class HmacHashUtil {
    private static final String SECRET_KEY = "gptonline-secret"; // properties 로 분리 가능

    public String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("해시 실패", e);
        }
    }
}
