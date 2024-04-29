package com.app.preorder.repository.member;

import com.app.preorder.entity.member.Salt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaltRepository extends JpaRepository<Salt, Long> {
}
