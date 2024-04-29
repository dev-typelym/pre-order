package com.app.preorder.domain.memberDTO;

import com.app.preorder.entity.embeddable.Address;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.member.Salt;
import com.app.preorder.type.Role;
import com.app.preorder.type.SleepType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.annotations.ColumnDefault;
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
    private SleepType memberSleep;

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
