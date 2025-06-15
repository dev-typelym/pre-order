package com.app.preorder.cartservice.controller;

import com.app.preorder.cartservice.client.MemberServiceClient;
import com.app.preorder.cartservice.dto.cart.AddCartItemRequest;
import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import com.app.preorder.cartservice.dto.member.MemberResponse;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.service.cart.CartService;
import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Slf4j
public class CartRestController {

    private final CartService cartService;
    private final MemberServiceClient memberFeignClient;

    @PostMapping("/me/items")
    public ResponseEntity<ApiResponse<Void>> addCartItem(@RequestBody AddCartItemRequest request) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        cartService.addCartItem(payload.getId(), request.getProductId(), request.getCount());
        return ResponseEntity.ok(ApiResponse.success(null, "장바구니에 상품을 추가했습니다."));
    }

    @PatchMapping("/me/items/{productId}/decrease")
    public ResponseEntity<ApiResponse<Void>> decreaseCartItem(@PathVariable Long productId, @RequestParam Long count) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        cartService.decreaseCartItem(payload.getId(), productId, count);
        return ResponseEntity.ok(ApiResponse.success(null, "장바구니 수량을 감소시켰습니다."));
    }

    // 카트 아이템 삭제
    @DeleteMapping("/me/items")
    public ResponseEntity<ApiResponse<Void>> deleteCartItems(@RequestBody List<Long> cartItemIds) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        cartService.deleteCartItems(payload.getId(), cartItemIds);
        return ResponseEntity.ok(ApiResponse.success(null, "장바구니 상품을 삭제했습니다."));
    }

    // 카트 목록 페이징 조회
    @GetMapping("/me/items")
    public Page<CartItemResponse> getCartItems(@RequestParam(defaultValue = "1") int page) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return cartService.getCartItemsWithPaging(page - 1, payload.getId());
    }

}
