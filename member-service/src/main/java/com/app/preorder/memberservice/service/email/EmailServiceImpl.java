package com.app.preorder.memberservice.service.email;

import com.app.preorder.common.exception.custom.EmailSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService{

    private final JavaMailSender emailSender;

    @Override
    public void sendSignupVerificationMail(String to, String verificationLink) {
        String subject = "[pre-order] 회원가입 인증 메일입니다.";
        String body = "아래 링크를 클릭하여 인증을 완료해주세요:\n\n" + verificationLink;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            emailSender.send(message);
        } catch (MailException e) {
            log.error("[EmailService] 이메일 전송 실패 - 대상: {}, 이유: {}", to, e.getMessage());
            throw new EmailSendFailedException("이메일 전송에 실패했습니다.", e);
        }
    }
}
