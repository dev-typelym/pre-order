package com.app.preorder.memberservice.dto;


import com.app.preorder.common.type.Role;
import com.app.preorder.memberservice.domain.vo.Address;

import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.domain.entity.Salt;
import com.app.preorder.memberservice.domain.type.MemberStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Data
@NoArgsConstructor
public class MemberDTO {

    private Long id;
    private String username;
    private String memberEmail;
    private String memberPassword;
    private String name;
    private String memberPhone;
    private Address memberAddress;
    private Salt salt;
    private LocalDateTime memberRegisterDate;
    private Role memberRole;
    private MemberStatus memberSleep;

    public MemberDTO(Member member) {
        this.username = member.getUsername();
        this.memberPhone = member.getMemberPhone();
        this.memberEmail = member.getMemberEmail();
    }

    @Builder
    public MemberDTO(Salt salt, String name, String memberEmail, String username, String memberPassword, String memberPhone, Address memberAddress) {
        this.memberEmail = memberEmail;
        this.username = username;
        this.name = name;
        this.memberPassword = memberPassword;
        this.memberPhone = memberPhone;
        this.memberAddress = memberAddress;
        this.salt = salt;
    }
}
