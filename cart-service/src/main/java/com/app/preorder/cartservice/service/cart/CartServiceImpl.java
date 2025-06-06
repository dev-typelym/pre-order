package com.app.preorder.cartservice.service.cart;


import com.app.preorder.cartservice.client.ProductServiceClient;
import com.app.preorder.cartservice.dto.cart.CartItemListDTO;
import com.app.preorder.cartservice.domain.entity.Cart;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.common.dto.ProductResponse;
import com.app.preorder.cartservice.repository.cartItem.CartItemRepository;
import com.app.preorder.cartservice.repository.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService{

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    // 카트 아이템 추가
    @Override
    public void addItem(Long memberId, Long productId, Long quantity) {
        Cart cart = cartRepository.findCartByMemberId(memberId);

        CartItem existingCartItem = null;
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProductId().equals(productId)) {
                existingCartItem = cartItem;
                break;
            }
        }

        if (existingCartItem != null) {
            // 이미 카트에 해당 제품이 있는 경우 수량을 증가시킴
            existingCartItem.updateCount(existingCartItem.getCount() + quantity);
        } else {
            CartItem cartItem = CartItem.builder().count(quantity).productId(productId).cart(cart).build();
            cart.getCartItems().add(cartItem);
        }
        cartRepository.save(cart);

    }

    // 카트 수량 감소
    @Override
    public void decreaseItem(Long memberId, Long productId, Long quantity) {
        Cart cart = cartRepository.findCartByMemberId(memberId);

        // 카트에서 해당 제품을 가진 아이템 찾기
        CartItem existingCartItem = null;
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProductId().equals(productId)) {
                existingCartItem = cartItem;
                break;
            }
        }

        if (existingCartItem != null) {
            // 현재 수량과 감소할 수량을 비교하여 새로운 수량 설정
            long newCount = existingCartItem.getCount() - quantity;
            if (newCount <= 0) {
                // 새로운 수량이 0 이하이면 해당 아이템을 삭제
                cart.getCartItems().remove(existingCartItem);
            } else {
                // 새로운 수량이 0 초과이면 수량 갱신
                existingCartItem.updateCount(newCount);
            }
            // 카트 저장
            cartRepository.save(cart);
        }
    }

    // 카트 아이템 삭제
    @Override
    public void deleteItem(List<String> cartItemIds) {
        cartItemIds.stream().map(cartItemId -> Long.parseLong(cartItemId)).forEach(cartItemRepository::deleteCartItemByIds_queryDSL);
    }

    // 카트 목록
    public Page<CartItemListDTO> getCartItemListWithPaging(int page, Long memberId) {
        // 1. 장바구니 아이템 조회
        Page<CartItem> cartItems = cartItemRepository.findAllCartItem_queryDSL(PageRequest.of(page, 5), memberId);

        // 2. productId 추출
        List<Long> productIds = new ArrayList<>();
        for (CartItem item : cartItems.getContent()) {
            productIds.add(item.getProductId());
        }

        // 3. ProductService 호출 (bulk 통신)
        List<ProductResponse> products = productServiceClient.getProductsByIds(productIds);

        // 4. Map으로 변환 (id → ProductResponse)
        Map<Long, ProductResponse> productMap = new HashMap<>();
        for (ProductResponse product : products) {
            productMap.put(product.getProductId(), product);
        }

        // 5. CartItem + Product 매핑해서 DTO 생성
        List<CartItemListDTO> dtoList = new ArrayList<>();
        for (CartItem item : cartItems.getContent()) {
            ProductResponse productResponse = productMap.get(item.getProductId());
            CartItemListDTO dto = toCartItemListDTO(item, productResponse);
            dtoList.add(dto);
        }

        // 6. 페이징 결과 반환
        return new PageImpl<>(dtoList, cartItems.getPageable(), cartItems.getTotalElements());
    }

    // 카트 하나 전체 정보
    @Override
    public CartItem getAllCartItemInfo(Long cartItemId) {

        CartItem cartItem = cartItemRepository.findCartItemById_queryDSL(cartItemId);
        return cartItem;
    }


}
