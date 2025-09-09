package com.app.preorder.cartservice.service.cart;

import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CartService {

    // 카트 존재 보장
    void ensureCartExists(Long memberId);

    // 카트에 아이템 추가
    void addCartItem(Long memberId, Long productId, Long quantity);

    // 카트 아이템 수량 감소
    void decreaseCartItem(Long memberId, Long productId, Long quantity);

    // 카트에 아이템 삭제 (선택 삭제)
    void deleteCartItems(Long memberId, List<Long> cartItemIds);

    // 카트 목록 조회
    Page<CartItemResponse> getCartItemsWithPaging(int page, Long memberId);

    // ====== ▼ 신규: 인박스에서 호출할 정리용 메서드들 ======

    /** 멤버 탈퇴 → 해당 멤버의 카트 전체 삭제 */
    void deleteCart(Long memberId);

    /** 상품 SOLD_OUT/비가용 → 전체 카트에서 해당 상품들 제거 */
    void deleteItemsByProductIds(List<Long> productIds);

    /** 주문완료(CART 주문) → 해당 멤버의 카트에서 특정 상품들만 제거 */
    void deleteItemsByMemberAndProducts(Long memberId, List<Long> productIds);
}
