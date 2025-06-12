package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.ForbiddenException;
import com.app.preorder.common.exception.custom.UserNotFoundException;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.infralib.util.HmacHashUtil;
import com.app.preorder.infralib.util.PasswordUtil;
import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.memberservice.client.CartServiceClient;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.SignupRequest;
import com.app.preorder.common.exception.custom.InvalidPasswordException;
import com.app.preorder.memberservice.dto.UpdateMemberRequest;
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
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final CartServiceClient cartServiceClient;
    private final MemberFactory memberFactory;
    private final EncryptUtil encryptUtil;
    private final PasswordUtil passwordUtil;
    private final HmacHashUtil hmacHashUtil;
    private final EmailService emailService;
    private final RedisUtil redisUtil;

    // 회원 찾기
    @Override
    public Member findByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId);
        if (member == null) {
            throw new UserNotFoundException("해당 회원을 찾을 수 없습니다.");
        }
        return member;
    }

    // 회원 정보 변경
    @Override
    @Transactional
    public void updateMember(UpdateMemberRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("회원이 존재하지 않습니다."));

        memberFactory.updateProfile(member, request);
    }

    // 비밀번호 변경
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

    //  회원가입
    @Override
    public void signUp(SignupRequest request) {
        Member member = memberFactory.createMember(request);
        memberRepository.save(member);

        cartServiceClient.createCart(member.getId());
    }

    // 중복 체크
    @Override
    public boolean isDuplicate(DuplicateCheckType type, String value) {
        String hash = hmacHashUtil.hmacSha256(value);
        return switch (type) {
            case LOGIN_ID -> memberRepository.existsByLoginIdHash(hash);
            case EMAIL -> memberRepository.existsByEmailHash(hash);
            case PHONE -> memberRepository.existsByPhoneHash(hash);
        };
    }

    // 비밀번호 검증
    @Override
    public MemberInternal verifyPasswordAndGetInfo(String username, String password) {
        Member member = memberRepository.findByLoginId(username);
        if (member == null) return null;

        boolean isValid = passwordUtil.verifyPassword(password, member.getPassword());
        if (!isValid) return null;

        return new MemberInternal(member.getId(), member.getLoginId(), member.getStatus(), member.getRole());
    }

    //  인증 메일 전송
    @Override
    public void sendSignupVerificationMail(Member member) {
        if (member == null) {
            throw new UserNotFoundException("멤버가 조회되지 않음");
        }

        String loginId = encryptUtil.decrypt(member.getLoginId());
        String email = encryptUtil.decrypt(member.getEmail());

        // 1. 랜덤 토큰 생성
        String token = UUID.randomUUID().toString();

        // 2. 인증 링크 생성
        String verificationLink = "http://localhost:8081/member/verify/" + token;

        // 3. Redis에 토큰 저장 (30분 유효)
        redisUtil.setDataExpire(token, loginId, 60 * 30L);

        // 4. 이메일 전송
        emailService.sendMail(
                email,
                "[pre-order] 회원가입 인증 메일입니다.",
                verificationLink
        );
    }

    //  인증 메일 확인
    @Override
    public void confirmEmailVerification(String key) {
        String loginId = redisUtil.getData(key);
        if (loginId == null) {
            throw new ForbiddenException("인증 링크가 만료되었거나 유효하지 않습니다.");
        }

        Member member = memberRepository.findByLoginId(loginId);
        if (member == null) {
            throw new UserNotFoundException("해당 회원을 찾을 수 없습니다.");
        }

        member.changeStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);

        redisUtil.deleteData(key);
    }

}
