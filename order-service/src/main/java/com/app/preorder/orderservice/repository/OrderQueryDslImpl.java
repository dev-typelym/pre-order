package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.app.preorder.entity.order.QOrderItem.orderItem;

@RequiredArgsConstructor
public class OrderQueryDslImpl implements OrderQueryDsl {

    private final JPAQueryFactory query;

    // 주문 목록
    @Override
    public Page<Order> findOrdersByMemberId(Long memberId, Pageable pageable) {
        List<Order> orders = queryFactory
                .selectFrom(order)
                .where(order.memberId.eq(memberId))
                .orderBy(order.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(order.memberId.eq(memberId))
                .fetchOne();

        return new PageImpl<>(orders, pageable, total != null ? total : 0);
    }

    @Override
    public Order findOrderItemsById(Long orderId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;

        return query.select(order)
                .from(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();
    }

}
