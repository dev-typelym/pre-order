package com.app.preorder.memberservice.config;

import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.infralib.util.HmacHashUtil;
import com.app.preorder.infralib.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;

@Configuration
public class CryptoConfig {

    @Value("${aes.key}")
    private String aesKey;

    @Value("${aes.salt}")
    private String aesSalt;

    @Value("${hmac.secret-key}")
    private String hmacKey;

    @Bean
    public PasswordUtil passwordUtil() {
        return new PasswordUtil();
    }

    @Bean
    public EncryptUtil encryptUtil() {
        AesBytesEncryptor encryptor = new AesBytesEncryptor(aesKey, aesSalt);
        return new EncryptUtil(encryptor);
    }

    @Bean
    public HmacHashUtil hmacHashUtil() {
        return new HmacHashUtil(hmacKey);
    }
}
