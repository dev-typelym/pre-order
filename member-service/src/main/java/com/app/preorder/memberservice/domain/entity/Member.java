package com.app.preorder.memberservice.domain.entity;


import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import com.app.preorder.memberservice.domain.vo.Address;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@Table(name = "TBL_MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String loginId;

    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    @Column(unique = true)
    private String phone;

    @Embedded
    @NotNull
    private Address address;

    @NotNull
    private LocalDateTime registeredAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Column(unique = true)
    private String loginIdHash;

    @Column(unique = true)
    private String emailHash;

    @Column(unique = true)
    private String phoneHash;

    // 비밀번호 변경
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 개인정보 변경
    public void updateProfile(String name, String email, String phone, Address address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    //
    public void changeStatus(MemberStatus newStatus) {
        this.status = newStatus;
    }

    @Builder
    public Member(String name, String email, String loginId, String password, String phone,
                  Address address, Role role, MemberStatus status, LocalDateTime registeredAt,
                  String loginIdHash, String emailHash, String phoneHash) {
        this.email = email;
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.status = status;
        this.registeredAt = registeredAt;
        this.loginIdHash = loginIdHash;
        this.emailHash = emailHash;
        this.phoneHash = phoneHash;
    }
}
