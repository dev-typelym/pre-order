package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberQueryDsl {
    Member findByUsername(String username);
}
