package com.app.preorder.repository.member;

import com.app.preorder.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberQueryDsl {
    Member findByUsername(String username);
}
