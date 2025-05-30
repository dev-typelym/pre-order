package com.app.preorder.memberservice.controller;

import com.app.preorder.memberservice.dto.MemberResponseDTO;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/members")
@RequiredArgsConstructor
@Slf4j
public class MemberInternalController {

    private final MemberRepository memberRepository;

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponseDTO> getMemberById(@PathVariable("id") Long id) {
        return memberRepository.findById(id)
                .map(member -> ResponseEntity.ok(MemberResponseDTO.from(member)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username")
    public ResponseEntity<MemberResponseDTO> getMemberByUsername(@RequestParam("username") String username) {
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(MemberResponseDTO.from(member));
    }
}
