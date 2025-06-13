package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Member findByLoginId(String loginId);

    //  아이디, 이메일, 휴대폰 중복 체크
    boolean existsByLoginIdHash(String loginIdHash);
    boolean existsByEmailHash(String emailHash);
    boolean existsByPhoneHash(String phoneHash);}
