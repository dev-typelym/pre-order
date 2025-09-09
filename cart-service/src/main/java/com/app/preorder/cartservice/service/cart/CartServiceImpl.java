package com.app.preorder.cartservice.service.cart;

import com.app.preorder.cartservice.client.ProductServiceClient;
import com.app.preorder.cartservice.domain.entity.Cart;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import com.app.preorder.cartservice.factory.CartFactory;
import com.app.preorder.cartservice.repository.cart.CartRepository;
import com.app.preorder.cartservice.repository.cartItem.CartItemRepository;
import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.CartNotFoundException;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.InvalidCartOperationException;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartFactory cartFactory;
    private final ProductServiceClient productServiceClient;

    // 카트 존재 보장
    @Override
    @Transactional
    public void ensureCartExists(Long memberId) {
        try {
            cartRepository.saveAndFlush(
                    Cart.builder().memberId(memberId).build()
            );
            if (log.isDebugEnabled()) {
                log.debug("[Cart] 카트 생성 -> memberId={}", memberId);
            }
        } catch (DataIntegrityViolationException e) {
            if (log.isDebugEnabled()) {
                log.debug("[Cart] 카트가 이미 존재함 -> memberId={}", memberId);
            }
        }
    }

    // 카트 아이템 추가
    @Override
    @Transactional
    @CircuitBreaker(name = "productClient")
    public void addCartItem(Long memberId, Long productId, Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidCartOperationException("수량은 1 이상이어야 합니다.");
        }

        // 카트 존재 보장 (Lazy Guard)
        ensureCartExists(memberId);

        // 상품 상태 확인
        ProductInternal product = productServiceClient.getProductsByIds(List.of(productId)).get(0);
        if (!product.getStatus().name().equals("ENABLED")) {
            throw new InvalidCartOperationException("상품이 판매 가능 상태가 아닙니다.");
        }

        Cart cart = cartRepository.findCartByMemberId(memberId)
                .orElseThrow(() -> new CartNotFoundException("회원의 장바구니가 존재하지 않습니다."));

        CartItem existingCartItem = cartItemRepository.findCartItemByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existingCartItem != null) {
            existingCartItem.updateCount(existingCartItem.getCount() + quantity);
        } else {
            CartItem cartItem = CartItem.builder()
                    .count(quantity)
                    .productId(productId)
                    .cart(cart)
                    .build();
            cart.getCartItems().add(cartItem);
        }
    }

    // 카트 수량 감소
    @Override
    @Transactional
    public void decreaseCartItem(Long memberId, Long productId, Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidCartOperationException("감소할 수량은 1 이상이어야 합니다.");
        }

        Cart cart = cartRepository.findCartByMemberId(memberId)
                .orElseThrow(() -> new CartNotFoundException("회원의 장바구니가 존재하지 않습니다."));

        CartItem existingCartItem = cartItemRepository.findCartItemByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new InvalidCartOperationException("장바구니에 해당 상품이 존재하지 않습니다."));

        long newCount = existingCartItem.getCount() - quantity;
        if (newCount <= 0) {
            cart.getCartItems().remove(existingCartItem);
        } else {
            existingCartItem.updateCount(newCount);
        }
    }

    // 카트 아이템 삭제 (선택 삭제)
    @Override
    @Transactional
    public void deleteCartItems(Long memberId, List<Long> cartItemIds) {
        Cart cart = cartRepository.findCartByMemberId(memberId)
                .orElseThrow(() -> new CartNotFoundException("회원의 장바구니가 존재하지 않습니다."));

        Set<Long> ownedItemIds = cart.getCartItems().stream()
                .map(CartItem::getId)
                .collect(Collectors.toSet());

        boolean hasInvalidItem = cartItemIds.stream()
                .anyMatch(id -> !ownedItemIds.contains(id));
        if (hasInvalidItem) {
            throw new InvalidCartOperationException("삭제하려는 상품 중 일부가 장바구니에 존재하지 않습니다.");
        }

        cartItemRepository.deleteCartItemsByIdsAndMemberId(cartItemIds, memberId);
    }

    // 카트 목록
    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "productClient")
    public Page<CartItemResponse> getCartItemsWithPaging(int page, Long memberId) {
        Page<CartItem> cartItems = cartItemRepository.findCartItemsByMemberId(PageRequest.of(page, 10), memberId);

        List<Long> productIds = cartItems.getContent().stream()
                .map(CartItem::getProductId)
                .toList();

        if (productIds.isEmpty()) {
            return Page.empty();
        }

        List<ProductInternal> products;
        try {
            products = productServiceClient.getProductsByIds(productIds);
        } catch (feign.FeignException e) {
            log.error("[CartService] 상품 정보 조회 실패 - 요청 productIds: {}, 이유: {}", productIds, e.getMessage());
            throw new FeignException("상품 서비스 통신 실패", e);
        }

        if (products.size() != productIds.size()) {
            throw new ProductNotFoundException("장바구니에 존재하지 않는 상품이 포함되어 있습니다.");
        }

        Map<Long, ProductInternal> productMap = products.stream()
                .collect(Collectors.toMap(ProductInternal::getId, p -> p));

        List<CartItemResponse> responseList = cartItems.getContent().stream()
                .map(item -> cartFactory.createCartItemResponse(item, productMap.get(item.getProductId())))
                .toList();

        return new PageImpl<>(responseList, cartItems.getPageable(), cartItems.getTotalElements());
    }

    // ===== ▼ 신규: 인박스용 정리 로직 구현 =====

    /** 멤버 탈퇴 → 해당 멤버의 카트 전체 삭제 */
    @Override
    @Transactional
    public void deleteCart(Long memberId) {
        Optional<Cart> cartOpt = cartRepository.findCartByMemberId(memberId);
        if (cartOpt.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("[Cart] deleteCart: 카트 없음 -> memberId={}", memberId);
            }
            return; // 멱등
        }
        Cart cart = cartOpt.get();
        // 연관관계에 cascade + orphanRemoval=true가 설정되어 있으면 아래 한 줄로 충분
        cartRepository.delete(cart);
        if (log.isInfoEnabled()) {
            log.info("[Cart] deleteCart: 장바구니 삭제 완료 -> memberId={}", memberId);
        }
    }

    /** 상품 SOLD_OUT/비가용 → 전체 카트에서 해당 상품들 제거 */
    @Override
    @Transactional
    public void deleteItemsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        // QueryDSL 벌크 삭제 (CartItemQueryDslImpl에서 구현 필요)
        cartItemRepository.deleteByProductIds(productIds);
        if (log.isInfoEnabled()) {
            log.info("[Cart] deleteItemsByProductIds: 전역 카트에서 상품들 삭제 -> productIds={}", productIds);
        }
    }

    /** 주문완료(CART) → 해당 멤버의 카트에서 특정 상품들만 제거 */
    @Override
    @Transactional
    public void deleteItemsByMemberAndProducts(Long memberId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        // QueryDSL 벌크 삭제 (CartItemQueryDslImpl에서 구현 필요)
        cartItemRepository.deleteByMemberIdAndProductIds(memberId, productIds);
        if (log.isInfoEnabled()) {
            log.info("[Cart] deleteItemsByMemberAndProducts: memberId={} productIds={}", memberId, productIds);
        }
    }
}
