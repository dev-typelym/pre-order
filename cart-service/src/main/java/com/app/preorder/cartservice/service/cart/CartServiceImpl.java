package com.app.preorder.cartservice.service.cart;


import com.app.preorder.cartservice.client.ProductServiceClient;
import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import com.app.preorder.cartservice.domain.entity.Cart;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.factory.CartFactory;
import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.cartservice.repository.cartItem.CartItemRepository;
import com.app.preorder.cartservice.repository.cart.CartRepository;
import com.app.preorder.common.exception.custom.CartNotFoundException;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.InvalidCartOperationException;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService{

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartFactory cartFactory;
    private final ProductServiceClient productServiceClient;

    // 회원가입시 카트 생성
    @Override
    public void createCartForMember(Long memberId) {
        Cart cart = Cart.builder()
                .memberId(memberId)
                .build();

        cartRepository.save(cart);
    }

    // 카트 아이템 추가
    @Override
    @Transactional
    public void addCartItem(Long memberId, Long productId, Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidCartOperationException("수량은 1 이상이어야 합니다.");
        }

        Cart cart = cartRepository.findCartByMemberId(memberId)
                .orElseThrow(() -> new CartNotFoundException("회원의 장바구니가 존재하지 않습니다."));

        CartItem existingCartItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
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

        CartItem existingCartItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new InvalidCartOperationException("장바구니에 해당 상품이 존재하지 않습니다."));

        long newCount = existingCartItem.getCount() - quantity;
        if (newCount <= 0) {
            cart.getCartItems().remove(existingCartItem);
        } else {
            existingCartItem.updateCount(newCount);
        }
    }

    // 카트 아이템 삭제
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
    public Page<CartItemResponse> getCartItemsWithPaging(int page, Long memberId) {
        Page<CartItem> cartItems = cartItemRepository.findCartItemsByMemberId(PageRequest.of(page, 5), memberId);

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
            // 명확한 서비스 단위 메시지 추가
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


}
