package com.app.preorder.service.member;

import com.app.preorder.domain.memberDTO.MemberDTO;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.member.Salt;
import com.app.preorder.repository.cart.CartRepository;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.repository.member.SaltRepository;
import com.app.preorder.service.email.EmailService;
import com.app.preorder.type.Role;
import com.app.preorder.type.SleepType;
import com.app.preorder.util.EncryptUtil;
import com.app.preorder.util.RedisUtil;
import com.app.preorder.util.SaltUtil;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public Member findByUsername(String username) throws NotFoundException {
        Member member = memberRepository.findByUsername(username);
        if(member == null) throw new NotFoundException("멤버가 조회되지 않음");
        return member;
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
    public void sendVerificationMail(Member member) throws NotFoundException {
        String VERIFICATION_LINK = "http://localhost:8081/member/verify/";
        if(member==null) throw new NotFoundException("멤버가 조회되지 않음");
        UUID uuid = UUID.randomUUID();
        redisUtil.setDataExpire(uuid.toString(),encryptUtil.decrypt(member.getUsername()), 60 * 30L);
        emailService.sendMail(encryptUtil.decrypt(member.getMemberEmail()),"[pre-order] 회원가입 인증메일입니다.",VERIFICATION_LINK+uuid.toString());
    }

    @Override
    public void verifyEmail(String key) throws NotFoundException {
        String memberId = redisUtil.getData(key);
        Member member = memberRepository.findByUsername(memberId);
        if(member==null) throw new NotFoundException("멤버가 조회되지않음");
        modifyUserRole(member,Role.ROLE_USER);
        redisUtil.deleteData(key);
    }

    @Override
    public void modifyUserRole(Member member, Role userRole){
        member.setMemberRole(userRole);
        memberRepository.save(member);
    }
}
