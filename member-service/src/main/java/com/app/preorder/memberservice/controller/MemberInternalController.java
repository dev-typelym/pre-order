package com.app.preorder.memberservice.controller;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.dto.VerifyPasswordRequest;
import com.app.preorder.memberservice.dto.MemberResponseDTO;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.exception.UserNotFoundException;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/members")
@RequiredArgsConstructor
@Slf4j
public class MemberInternalController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    // Feign 내부용: ID 기반 회원 조회
    @GetMapping("/id/{id}")
    public MemberResponseDTO getMemberById(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).orElse(null);
        if (member == null) {
            throw new UserNotFoundException("존재하지 않는 회원입니다.");
        }
        return MemberResponseDTO.from(member);
    }

    // Feign 내부용: username 기반 회원 조회 (ex. 장바구니, 주문)
    @GetMapping("/username/{username}")
    public MemberResponseDTO getMemberByUsername(@PathVariable String username) {
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            throw new UserNotFoundException("존재하지 않는 회원입니다.");
        }
        return MemberResponseDTO.from(member);
    }

    @PostMapping("/verify-password")
    public MemberInternal verifyPassword(@RequestBody VerifyPasswordRequest request) {
        return memberService.verifyPasswordAndGetInfo(request.getUsername(), request.getPassword());
    }

}
