package com.app.preorder.cartservice.service.cart;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.productDTO.ProductListDTO;
import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.product.Product;
import com.app.preorder.repository.cart.CartItemRepository;
import com.app.preorder.repository.cart.CartRepository;
import com.app.preorder.repository.product.ProductRepository;
import com.app.preorder.type.CatergoryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static io.lettuce.core.ShutdownArgs.Builder.save;

@Service
@Slf4j
public class CartServiceImpl implements CartService{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    // 카트 아이템 추가
    @Override
    public void addItem(Long memberId, Long productId, Long quantity) {
        Cart cart = cartRepository.findCartByMemberId(memberId);
        Product product = productRepository.findProductByProductId_queryDSL(productId);
        CartItem existingCartItem = null;
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProduct().getId().equals(productId)) {
                existingCartItem = cartItem;
                break;
            }
        }

        if (existingCartItem != null) {
            // 이미 카트에 해당 제품이 있는 경우 수량을 증가시킴
            existingCartItem.updateCount(existingCartItem.getCount() + quantity);
        } else {
            CartItem cartItem = CartItem.builder().count(quantity).product(product).cart(cart).build();
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
            if (cartItem.getProduct().getId().equals(productId)) {
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
    @Override
    public Page<CartItemListDTO> getCartItemListWithPaging(int page, Long memberId) {
        Page<CartItem> cartItems = cartItemRepository.findAllCartItem_queryDSL(PageRequest.of(page, 5), memberId);
        List<CartItemListDTO> cartItemListDTOS = cartItems.getContent().stream()
                .map(this::toCartItemListDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(cartItemListDTOS, cartItems.getPageable(), cartItems.getTotalElements());
    }

    // 카트 하나 전체 정보
    @Override
    public CartItem getAllCartItemInfo(Long cartItemId) {

        CartItem cartItem = cartItemRepository.findCartItemById_queryDSL(cartItemId);
        return cartItem;
    }


}
