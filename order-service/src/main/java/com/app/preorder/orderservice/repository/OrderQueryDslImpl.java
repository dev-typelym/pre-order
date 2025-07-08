package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.QOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RequiredArgsConstructor
public class OrderQueryDslImpl implements OrderQueryDsl {

    private final JPAQueryFactory query;

    // 주문 목록
    @Override
    public Page<Order> findOrdersByMemberId(Long memberId, Pageable pageable) {
        QOrder order = QOrder.order;

        List<Order> orders = query
                .selectFrom(order)
                .where(order.memberId.eq(memberId))
                .orderBy(order.orderDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = query
                .select(order.count())
                .from(order)
                .where(order.memberId.eq(memberId))
                .fetchOne();

        return new PageImpl<>(orders, pageable, total != null ? total : 0);
    }


}
