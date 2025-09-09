package com.app.preorder.cartservice.repository.cartItem;

import com.app.preorder.cartservice.domain.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CartItemQueryDsl {

    // 카트아이템 삭제 (선택 삭제)
    void deleteCartItemsByIdsAndMemberId(List<Long> cartItemIds, Long memberId);

    // 상품 목록 조회
    Page<CartItem> findCartItemsByMemberId(Pageable pageable, Long memberId);

    // ===== ▼ 추가: 인박스 정리용 벌크 삭제 =====

    /** 전역 카트에서 특정 상품들 제거 (SOLD_OUT 등) */
    void deleteByProductIds(List<Long> ids);

    /** 특정 멤버의 카트에서 특정 상품들만 제거 (ORDER_COMPLETED: CART) */
    void deleteByMemberIdAndProductIds(Long memberId, List<Long> ids);
}
