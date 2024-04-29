package com.app.preorder.entity.member;

import com.app.preorder.entity.audit.Period;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.embeddable.Address;
import com.app.preorder.entity.like.ProductLike;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.product.Product;
import com.app.preorder.type.Role;
import com.app.preorder.type.SleepType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString(exclude = {"randomKeys"})
@Table(name = "TBL_MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String  username;

    @NotBlank @Column(unique = true)
    private String  memberEmail;

    @NotBlank
    private String memberPassword;

    @NotBlank
    private String name;

    @NotBlank @Column(unique = true)
    private String memberPhone;

    @Embedded @NotNull
    private Address memberAddress;

    @NotNull
    private LocalDateTime memberRegisterDate;

    //    로그인 시 멤버 종류 나누기 위한 column
    @NotNull
    @Enumerated(EnumType.STRING)
    private Role memberRole;

    /* 탈퇴(sleep)가 true*/
    @NotNull @Enumerated(EnumType.STRING)
    private SleepType memberSleep;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    private List<Product> products;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    // Member의 입장에서, Order은 일대다 관계(연관관계 표시)
    // Order table의 Member field에 의해 매핑됨(Mapped). 연관관계의 주인은 Member가 아닌 Order
    private List<Order> orders = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    private List<RandomKey> randomKeys;

    @OneToMany(mappedBy = "member")
    private List<ProductLike> productLikes = new ArrayList<>();

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Cart cart;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "salt_id")
    private Salt salt;

    public void updatePassword(String memberPassword){
        this.memberPassword = memberPassword;
    }


    public Member update(String username, String memberPhone, String memberEmail){
        this.username = username;
        this.memberPhone = memberPhone;
        this.memberEmail = memberEmail;
        this.memberRole = Role.ROLE_USER;

        return this;
    }


    @Builder
    public Member(Salt salt, String name, String memberEmail, String username, String memberPassword, String memberPhone, Address memberAddress, Role memberRole, SleepType memberSleep, LocalDateTime memberRegisterDate) {
        this.memberEmail = memberEmail;
        this.username = username;
        this.name = name;
        this.memberPassword = memberPassword;
        this.memberPhone = memberPhone;
        this.memberAddress = memberAddress;
        this.salt = salt;
        this.memberRole = memberRole;
        this.memberSleep = memberSleep;
        this.memberRegisterDate = memberRegisterDate;
    }


    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public void setMemberPassword(String memberPassword) {
        this.memberPassword = memberPassword;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public void setMemberAddress(Address memberAddress) {
        this.memberAddress = memberAddress;
    }

    public void setMemberSleep(SleepType memberSleep) {
        this.memberSleep = memberSleep;
    }


}
