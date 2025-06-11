package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.memberservice.domain.vo.Address;
import com.app.preorder.memberservice.dto.MemberDTO;
import com.app.preorder.common.type.Role;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.domain.entity.Salt;
import com.app.preorder.memberservice.dto.UpdateMemberInfo;
import com.app.preorder.memberservice.exception.InvalidPasswordException;
import com.app.preorder.memberservice.exception.UserNotFoundException;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.email.EmailService;
import com.app.preorder.memberservice.util.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@Slf4j
public class MemberServiceImpl implements MemberService{

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordUtil passwordUtil;


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
    public void updateMember(UpdateMemberInfo updateMemberInfo, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("회원이 존재하지 않습니다."));

        Address address = new Address(
                updateMemberInfo.getAddress(),
                updateMemberInfo.getAddressDetail(),
                updateMemberInfo.getAddressSubDetail(),
                updateMemberInfo.getPostCode()
        );

        member.updateProfile(
                updateMemberInfo.getName(),
                updateMemberInfo.getEmail(),
                updateMemberInfo.getPhone(),
                address
        );
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
        member.updatePassword(encodedNewPassword);  // 엔티티 도메인 메서드로 변경
    }

    // 회원가입
    @Override
    public void signUpUser(MemberDTO memberDTO) {
        String password = memberDTO.getMemberPassword();
        String salt = saltUtil.genSalt();
        memberDTO.setSalt(new Salt(salt));
        memberDTO.setMemberPassword(saltUtil.encodePassword(salt,password));
        String encodedEmail = encryptUtil.encrypt(memberDTO.getMemberEmail());
        String encodedName = encryptUtil.encrypt(memberDTO.getName());
        String encodedAddressCity = encryptUtil.encrypt(memberDTO.getMemberAddress().getAddress());
        String encodedAddressDetail = encryptUtil.encrypt(memberDTO.getMemberAddress().getAddressDetail());
        String encodedAddressSubDetail = encryptUtil.encrypt(memberDTO.getMemberAddress().getAddressSubDetail());
        String encodedAddressPostCode = encryptUtil.encrypt(memberDTO.getMemberAddress().getPostcode());
        String encodedPhone = encryptUtil.encrypt(memberDTO.getMemberPhone());
        String encodedUserName = encryptUtil.encrypt(memberDTO.getUsername());
        memberDTO.setMemberEmail(encodedEmail);
        memberDTO.setName(encodedName);
        memberDTO.getMemberAddress().setAddress(encodedAddressCity);
        memberDTO.getMemberAddress().setAddressDetail(encodedAddressDetail);
        memberDTO.getMemberAddress().setAddressSubDetail(encodedAddressSubDetail);
        memberDTO.getMemberAddress().setPostcode(encodedAddressPostCode);
        memberDTO.setUsername(encodedUserName);
        memberDTO.setMemberPhone(encodedPhone);
        memberDTO.setMemberRole(Role.ROLE_NOT_PERMITTED);
        memberDTO.setMemberSleep(MemberStatus.ACTIVE);
        memberDTO.setMemberRegisterDate(LocalDateTime.now());
        Member member = toMemberEntity(memberDTO);
        Cart cart = Cart.builder().member(member).build();
        memberRepository.save(member);
        cartRepository.save(cart);
    }



    // 아이디 중복 체크
    @Override
    public Long overlapByMemberId(String username) {
        return (memberRepository.overlapByMemberId_QueryDSL(username));
    }


    // 이메일 중복 체크
    @Override
    public Long overlapByMemberEmail(String memberEmail) {
        return (memberRepository.overlapByMemberEmail_QueryDSL(memberEmail));
    }

    // 휴대폰 중복 체크
    @Override
    public Long overlapByMemberPhone(String memberPhone) {
        return (memberRepository.overlapByMemberPhone_QueryDSL(memberPhone));
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
    public void sendVerificationMail(Member member) {
        if (member == null) {
            throw new UserNotFoundException("멤버가 조회되지 않음");
        }

        String loginId = encryptUtil.decrypt(member.getLoginId());
        String email = encryptUtil.decrypt(member.getMemberEmail());

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
    public void verifyEmail(String key) {
        String memberId = redisUtil.getData(key);
        Member member = memberRepository.findByUsername(memberId);
        if (member == null) throw new UserNotFoundException("멤버가 조회되지 않음");
        modifyMemberStatus(member, MemberStatus.ACTIVE);
        redisUtil.deleteData(key);
    }

    private void modifyMemberStatus(Member member, MemberStatus memberStatus) {
        member.setMemberStatus(memberStatus);
        memberRepository.save(member);
    }
}
