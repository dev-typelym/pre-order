package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    /** 로그인 아이디로 회원 조회 */
    Optional<Member> findByLoginId(String loginId);

    /** 로그인 아이디 중복 체크 */
    boolean existsByLoginIdHash(String loginIdHash);

    /** 이메일 중복 체크 */
    boolean existsByEmailHash(String emailHash);

    /** 전화번호 중복 체크 */
    boolean existsByPhoneHash(String phoneHash);
}