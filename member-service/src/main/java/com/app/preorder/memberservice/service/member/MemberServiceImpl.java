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
import com.app.preorder.memberservice.factory.MemberFactory;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.email.EmailService;
import com.app.preorder.common.type.DuplicateCheckType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    // 로그인 아이디로 회원 조회
    @Override
    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("해당 회원을 찾을 수 없습니다."));
    }

    // 로그인 아이디와 비밀번호 검증 후 내부 회원 정보 반환
    @Override
    public MemberInternal verifyPasswordAndGetInfo(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordUtil.verifyPassword(password, member.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return new MemberInternal(member.getId(), member.getLoginId(), member.getStatus(), member.getRole());
    }

    // 회원가입 처리 및 카트 생성
    @Override
    @Transactional(rollbackOn = Exception.class)
    public void register(SignupRequest request) {
        Member member = memberFactory.createMember(request);
        memberRepository.save(member);

        try {
            cartServiceClient.createCart(member.getId());
        } catch (Exception e) {
            throw new FeignException("카트 서비스 호출 실패", e);
        }
    }

    // 회원 정보 수정
    @Override
    @Transactional
    public void updateMember(UpdateMemberRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("회원이 존재하지 않습니다."));

        memberFactory.updateProfile(member, request);
    }

    // 회원 비밀번호 변경
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

    // 아이디, 이메일, 전화번호 중복 여부 확인
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

    // 이메일 인증 메일 전송
    @Override
    public void sendSignupVerificationMail(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        String decryptedEmail = encryptUtil.decrypt(member.getEmail());
        String token = UUID.randomUUID().toString();
        String verificationLink = "http://localhost:8081/member/verify/" + token;

        redisUtil.setDataExpire(token, loginId, 60 * 30L);
        emailService.sendSignupVerificationMail(decryptedEmail, verificationLink);
    }

    // 이메일 인증 확인 및 상태 변경
    @Override
    @Transactional
    public void confirmEmailVerification(String key) {
        String loginId = redisUtil.getData(key);
        if (loginId == null) {
            throw new ForbiddenException("인증 링크가 만료되었거나 유효하지 않습니다.");
        }

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("해당 회원을 찾을 수 없습니다."));

        member.changeStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);

        redisUtil.deleteData(key);
    }
}
