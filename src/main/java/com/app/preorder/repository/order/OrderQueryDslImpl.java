package com.app.preorder.repository.order;

import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.QOrder;
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
    public Page<Order> findAllOrder_queryDSL(Pageable pageable, Long memberId) {

        QOrder order = QOrder.order;
        List<Order> foundCartItem = query.select(order)
                .from(order)
                .where(order.member.id.eq(memberId))
                .orderBy(order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(order.count())
                .from(order)
                .where(order.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(foundCartItem, pageable, count);
    }

    @Override
    public Order findOrderItemsByOrderId_queryDSL(Long orderId){
        QOrder order = QOrder.order;

        return query.select(order)
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();
    }
}
