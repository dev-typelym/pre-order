package com.app.preorder.cartservice.repository.cartItem;

import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.domain.entity.QCartItem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.app.preorder.cartservice.domain.entity.QCartItem.cartItem;

@RequiredArgsConstructor
public class CartItemQueryDslImpl implements CartItemQueryDsl {

    private final JPAQueryFactory query;

    // 카트 아이템 삭제 (선택 삭제)
    @Override
    public void deleteCartItemsByIdsAndMemberId(List<Long> cartItemIds, Long memberId) {
        query.delete(cartItem)
                .where(
                        cartItem.id.in(cartItemIds)
                                .and(cartItem.cart.memberId.eq(memberId))
                )
                .execute();
    }

    // 카트 아이템 목록
    @Override
    public Page<CartItem> findCartItemsByMemberId(Pageable pageable, Long memberId) {
        QCartItem c = QCartItem.cartItem;

        List<CartItem> content = query.selectFrom(c)
                .where(c.cart.memberId.eq(memberId))
                .orderBy(c.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(c.count())
                .from(c)
                .where(c.cart.memberId.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, count == null ? 0 : count);
    }

    // ===== ▼ 추가: 인박스 정리용 벌크 삭제 =====

    /** 전역 카트에서 특정 상품들 제거 (SOLD_OUT 등) */
    @Override
    public void deleteByProductIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        query.delete(cartItem)
                .where(cartItem.productId.in(ids))
                .execute();
    }

    /** 특정 멤버의 카트에서 특정 상품들만 제거 (ORDER_COMPLETED: CART) */
    @Override
    public void deleteByMemberIdAndProductIds(Long memberId, List<Long> ids) {
        if (memberId == null || ids == null || ids.isEmpty()) return;
        query.delete(cartItem)
                .where(
                        cartItem.cart.memberId.eq(memberId)
                                .and(cartItem.productId.in(ids))
                )
                .execute();
    }
}
