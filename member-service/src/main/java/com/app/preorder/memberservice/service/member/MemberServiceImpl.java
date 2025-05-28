package com.app.preorder.memberservice.service.member;


import com.app.preorder.memberservice.domain.MemberDTO;
import com.app.preorder.memberservice.domain.type.Role;
import com.app.preorder.memberservice.entity.Member;
import com.app.preorder.memberservice.entity.Salt;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.repository.SaltRepository;
import com.app.preorder.memberservice.service.email.EmailService;
import com.app.preorder.memberservice.domain.type.SleepType;
import com.app.preorder.memberservice.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@Slf4j
public class MemberServiceImpl implements MemberService{

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SaltRepository saltRepository;

    @Autowired
    private SaltUtil saltUtil;

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

    @Override
    public Member findByUsername(String username) throws ChangeSetPersister.NotFoundException {
        Member member = memberRepository.findByUsername(username);
        if(member == null) throw new ChangeSetPersister.NotFoundException("멤버가 조회되지 않음");
        return member;
    }

    // 회원 정보 변경
    @Override
    public void changeMemberInfo(String name, String email, String phone, String address, String addressDetail, String addressSubDetail, String postCode, Long memberId) {
        memberRepository.changeMemberInfo_QueryDSL(name, email, phone, address, addressDetail, addressSubDetail, postCode, memberId);
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
        memberDTO.setMemberSleep(SleepType.AWAKE);
        memberDTO.setMemberRegisterDate(LocalDateTime.now());
        Member member = toMemberEntity(memberDTO);
        Cart cart = Cart.builder().member(member).build();
        memberRepository.save(member);
        cartRepository.save(cart);
    }

    // 로그인
    @Override
    public Member loginUser(String id, String password) throws Exception{
        Member member = memberRepository.findByUsername(encryptUtil.encrypt(id));
        log.info(encryptUtil.encrypt(id));
        if(member==null) throw new Exception ("멤버가 조회되지 않음");
        String salt = member.getSalt().getSalt();
        password = saltUtil.encodePassword(salt,password);
        if(!member.getMemberPassword().equals(password))
            throw new Exception ("비밀번호가 틀립니다.");
        return member;
    }

    @Override
    public boolean logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // 클라이언트에서 받은 토큰을 가져옵니다.
        Cookie jwtToken = cookieUtil.getCookie(request, JwtUtil.ACCESS_TOKEN_NAME);

        // 토큰이 존재하면 만료시킵니다.
        if (jwtToken != null) {
            jwtUtil.invalidateToken(jwtToken.getValue());

            // Redis에 토큰을 추가로 저장하여 무효화합니다.
            redisUtil.setDataExpire(jwtToken.getValue(), "INVALIDATED", JwtUtil.TOKEN_VALIDATION_SECOND);

            // 클라이언트에게 새로운 토큰을 제공하지 않도록 만료된 토큰을 삭제합니다.
            Cookie expiredCookie = new Cookie(JwtUtil.ACCESS_TOKEN_NAME, null);
            expiredCookie.setPath("/");
            expiredCookie.setMaxAge(0);
            response.addCookie(expiredCookie);

            return true;
        } else {
            return false;
        }
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

    // 이메일 인증
    @Override
    public void sendVerificationMail(Member member) throws ChangeSetPersister.NotFoundException {
        String VERIFICATION_LINK = "http://localhost:8081/member/verify/";
        if(member==null) throw new ChangeSetPersister.NotFoundException("멤버가 조회되지 않음");
        UUID uuid = UUID.randomUUID();
        redisUtil.setDataExpire(uuid.toString(),encryptUtil.decrypt(member.getUsername()), 60 * 30L);
        emailService.sendMail(encryptUtil.decrypt(member.getMemberEmail()),"[pre-order] 회원가입 인증메일입니다.",VERIFICATION_LINK+uuid.toString());
    }

    @Override
    public void verifyEmail(String key) throws ChangeSetPersister.NotFoundException {
        String memberId = redisUtil.getData(key);
        Member member = memberRepository.findByUsername(memberId);
        if(member==null) throw new ChangeSetPersister.NotFoundException("멤버가 조회되지않음");
        modifyUserRole(member,Role.ROLE_USER);
        redisUtil.deleteData(key);
    }

    @Override
    public void modifyUserRole(Member member, Role userRole){
        member.setMemberRole(userRole);
        memberRepository.save(member);
    }
}
