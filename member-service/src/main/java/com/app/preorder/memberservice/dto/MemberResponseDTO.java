package com.app.preorder.memberservice.dto;

import com.app.preorder.memberservice.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;

    public static MemberResponseDTO from(Member member) {
        return MemberResponseDTO.builder()
                .id(member.getId())
                .username(member.getUsername())
                .email(member.getMemberEmail())
                .phone(member.getMemberPhone()) // Member 엔티티에 따라 조정
                .role(member.getMemberRole().name()) // ENUM이라면 name()
                .build();
    }
}
