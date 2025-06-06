package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.UserNotFoundException;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.util.RedisUtil;
import com.app.preorder.memberservice.dto.MemberDTO;
import com.app.preorder.common.type.Role;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.domain.entity.Salt;
import com.app.preorder.memberservice.dto.UpdateMemberInfo;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.email.EmailService;
import com.app.preorder.memberservice.domain.type.MemberStatus;
import com.app.preorder.memberservice.util.*;
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
    private RedisUtil redisUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private PasswordUtil passwordUtil;


    @Override
    public Member findByUsername(String username) throws ChangeSetPersister.NotFoundException {
        Member member = memberRepository.findByUsername(username);
        if(member == null) throw new ChangeSetPersister.NotFoundException("멤버가 조회되지 않음");
        return member;
    }

    // 회원 정보 변경
    @Override
    public void updateMember(UpdateMemberInfo updateMemberInfo, Long memberId) {
        memberRepository.updateMemberById(updateMemberInfo, memberId);
    }

    // 비밀번호 변경
    @Override
    public void changePassword(String password, Long memberId) {
        memberRepository.changePassword_QueryDSL(password, memberId);
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
        Member member = memberRepository.findByUsername(username);
        if (member == null) return null;

        boolean isValid = passwordUtil.verifyPassword(password, member.getMemberPassword());
        if (!isValid) return null;

        return new MemberInternal(member.getId(), member.getUsername(), member.getMemberStatus());
    }

    // 이메일 인증
    @Override
    public void sendVerificationMail(Member member) {
        String VERIFICATION_LINK = "http://localhost:8081/member/verify/";
        if (member == null) throw new UserNotFoundException("멤버가 조회되지 않음");
        UUID uuid = UUID.randomUUID();
        redisUtil.setDataExpire(uuid.toString(), encryptUtil.decrypt(member.getUsername()), 60 * 30L);
        emailService.sendMail(
                encryptUtil.decrypt(member.getMemberEmail()),
                "[pre-order] 회원가입 인증메일입니다.",
                VERIFICATION_LINK + uuid.toString()
        );
    }

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
