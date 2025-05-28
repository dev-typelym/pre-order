package com.app.preorder.memberservice.repository;

import com.app.preorder.memberservice.entity.Salt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaltRepository extends JpaRepository<Salt, Long> {
}