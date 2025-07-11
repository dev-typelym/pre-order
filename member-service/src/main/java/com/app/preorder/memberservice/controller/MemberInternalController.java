package com.app.preorder.memberservice.controller;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import com.app.preorder.common.exception.custom.UserNotFoundException;
import com.app.preorder.memberservice.domain.entity.Member;
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

    /** [Feign] ID 기반 회원 정보 조회 */

    @GetMapping("/{id}")
    public MemberInternal getMemberById(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        return new MemberInternal(
                member.getId(),
                member.getLoginId(),
                member.getStatus(),
                member.getRole()
        );
    }

    /** [Feign] 아이디 + 비밀번호 일치 여부 검증 */

    @PostMapping("/verify-password")
    public MemberInternal verifyPassword(@RequestBody VerifyPasswordInternal request) {
        return memberService.verifyPasswordAndGetInfo(request.getUsername(), request.getPassword());
    }
}
