package com.app.preorder.entity.order;


import com.app.preorder.entity.audit.Period;
import com.app.preorder.entity.member.Member;
import com.app.preorder.type.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(name = "tbl_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends Period {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval= true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    // 연관관계 메서드
    public void setMember(Member member) {
        this.member = member;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

}
