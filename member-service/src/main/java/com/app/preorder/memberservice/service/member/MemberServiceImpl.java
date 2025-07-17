package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.*;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.infralib.util.HmacHashUtil;
import com.app.preorder.infralib.util.PasswordUtil;
import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.memberservice.client.CartServiceClient;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.request.DuplicateCheckRequest;
import com.app.preorder.memberservice.dto.request.SignupRequest;
import com.app.preorder.memberservice.dto.request.UpdateMemberRequest;
import com.app.preorder.memberservice.dto.response.MemberDetailResponse;
import com.app.preorder.memberservice.factory.MemberFactory;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.email.EmailService;
import com.app.preorder.common.type.DuplicateCheckType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final CartServiceClient cartServiceClient;
    private final MemberFactory memberFactory;
    private final EncryptUtil encryptUtil;
    private final PasswordUtil passwordUtil;
    private final HmacHashUtil hmacHashUtil;
    private final EmailService emailService;
    private final RedisUtil redisUtil;

    @Value("${email.auto-verify:false}")
    private boolean autoVerify;

    //  로그인 아이디로 회원 조회
    @Override
    @Transactional(readOnly = true)
    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("해당 회원을 찾을 수 없습니다."));
    }

    //  로그인 아이디와 비밀번호 검증 후 내부 회원 정보 반환
    @Override
    public MemberInternal verifyPasswordAndGetInfo(String loginId, String currentPassword) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordUtil.verifyPassword(currentPassword, member.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return new MemberInternal(member.getId(), member.getLoginId(), member.getStatus(), member.getRole());
    }

    //  회원가입 처리 및 인증메일 전송
    @Transactional
    public void signup(SignupRequest request) {
        Member member = memberFactory.createMember(request);

        if (memberRepository.existsByLoginIdHash(hmacHashUtil.hmacSha256(member.getLoginId()))) {
            throw new DuplicateValueException("이미 사용 중인 로그인 ID입니다.");
        }
        if (memberRepository.existsByEmailHash(hmacHashUtil.hmacSha256(member.getEmail()))) {
            throw new DuplicateValueException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByPhoneHash(hmacHashUtil.hmacSha256(member.getPhone()))) {
            throw new DuplicateValueException("이미 사용 중인 전화번호입니다.");
        }

        if (autoVerify) {
            member.changeStatus(MemberStatus.ACTIVE);
        }
        memberRepository.save(member);

        if (autoVerify) {
            try {
                cartServiceClient.createCart(member.getId());
            } catch (FeignException e) {
                log.error("카트 생성 실패", e);
                redisUtil.addSet("failed_cart_member_ids", member.getId().toString());
            }
        } else {
            sendSignupVerificationMail(member.getLoginId());
        }
    }

    //  회원가입 처리 및 인증메일 전송
    public void resendVerificationEmail(String loginId) {
        // 오늘 날짜 문자열
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // loginId로 Member 조회 후 memberId 사용
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
        Long memberId = member.getId();

        String key = "resend_count:" + memberId + ":" + today;

        //  자정까지 남은 초 계산
        long secondsUntilMidnight = Duration.between(
                LocalDateTime.now(),
                LocalDate.now().plusDays(1).atStartOfDay()
        ).getSeconds();

        //  Redis count 증가 (첫 호출이면 TTL 설정)
        Long count = redisUtil.incrementCount(key, secondsUntilMidnight);

        if (count > 3) {
            throw new EmailResendLimitException("오늘은 더 이상 재발송할 수 없습니다.");
        }

        //  인증 메일 발송
        sendSignupVerificationMail(loginId);
    }

    //  내 정보 조회
    @Transactional(readOnly = true)
    public MemberDetailResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
        return MemberDetailResponse.of(member, encryptUtil);
    }

    //  회원 정보 수정
    @Override
    @Transactional
    public void updateMember(UpdateMemberRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("회원이 존재하지 않습니다."));

        memberFactory.updateProfile(member, request);
    }

    //  회원 비밀번호 변경
    @Override
    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (!passwordUtil.verifyPassword(currentPassword, member.getPassword())) {
            throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordUtil.encodePassword(newPassword);
        member.updatePassword(encodedNewPassword);
    }

    //  아이디, 이메일, 전화번호 중복 여부 확인
    @Override
    public String checkDuplicate(DuplicateCheckRequest request) {
        DuplicateCheckType type = DuplicateCheckType.from(request.getType());
        String hash = hmacHashUtil.hmacSha256(request.getValue());

        boolean isDuplicate = switch (type) {
            case LOGIN_ID -> memberRepository.existsByLoginIdHash(hash);
            case EMAIL -> memberRepository.existsByEmailHash(hash);
            case PHONE -> memberRepository.existsByPhoneHash(hash);
        };

        if (isDuplicate) {
            throw new DuplicateValueException("이미 사용 중인 " + type.getDisplayName() + "입니다.");
        }

        return "사용 가능한 " + type.getDisplayName() + "입니다.";
    }

    //  이메일 인증 메일 전송
    @Override
    public void sendSignupVerificationMail(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        String decryptedEmail = encryptUtil.decrypt(member.getEmail());
        String token = UUID.randomUUID().toString();
        String verificationLink = "http://localhost:8081/member/verify/" + token;

        redisUtil.setDataExpire(token, String.valueOf(member.getId()), 60 * 30L);
        emailService.sendSignupVerificationMail(decryptedEmail, verificationLink);
    }

    //  이메일 인증 확인 및 카트 생성
    @Override
    @Transactional
    public void confirmEmailVerification(String key) {
        String memberIdStr = redisUtil.getData(key);
        if (memberIdStr == null) {
            log.warn("[MemberService] 이메일 인증 실패 - key: {}", key);
            throw new ForbiddenException("인증 링크가 만료되었거나 유효하지 않습니다.");
        }

        Long memberId = Long.parseLong(memberIdStr);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("해당 회원을 찾을 수 없습니다."));

        member.changeStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);

        //  인증 완료 후 카트 생성
        try {
            cartServiceClient.createCart(memberId); // ✅ 외부 요청
        } catch (FeignException e) {
            log.error("카트 생성 실패", e);
            redisUtil.addSet("failed_cart_member_ids", memberId.toString());
        }

        redisUtil.deleteData(key);
    }

    //  회원탈퇴
    @Transactional
    public void deleteMember(Long memberId, String currentPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        if (!passwordUtil.verifyPassword(currentPassword, member.getPassword())) {
            throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        member.changeStatus(MemberStatus.INACTIVE);
    }
}
