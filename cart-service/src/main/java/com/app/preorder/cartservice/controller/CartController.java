package com.app.preorder.cartservice.controller;

import com.app.preorder.cartservice.client.MemberServiceClient;
import com.app.preorder.cartservice.dto.cart.CartItemListDTO;
import com.app.preorder.cartservice.dto.member.MemberResponse;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final MemberServiceClient memberFeignClient;

    // 카트에 아이템 추가
    @PostMapping("/cartItem/add/{productId}")
    public void addItemToCart(@PathVariable Long productId, @RequestParam Long count) {
        // 인증된 사용자 이름을 SecurityContext에서 추출
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // FeignClient를 통해 member-service에서 회원 정보 조회
        MemberResponse member = memberFeignClient.getMemberByUsername(username);

        // 장바구니 서비스 호출
        cartService.addItem(member.getId(), productId, count);
    }

    // 카트에 아이템 수량 감소
    @PostMapping("/cartItem/decrease/{productId}")
    public void decreaseItemToCart(@PathVariable Long productId, @RequestParam Long count) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberResponse member = memberFeignClient.getMemberByUsername(username);
        cartService.decreaseItem(member.getId(), productId, count);
    }

    // 카트 아이템 삭제
    @PostMapping("/cartItem/delete")
    public void deleteItemFromCart(@RequestParam("checkedIds[]") List<String> checkIds) {
        cartService.deleteItem(checkIds);
    }

    // 카트 목록 페이징 조회
    @GetMapping("/cartItemList/{page}")
    public Page<CartItemListDTO> getCartItemList(@PathVariable int page) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberResponse member = memberFeignClient.getMemberByUsername(username);
        return cartService.getCartItemListWithPaging(page - 1, member.getId());
    }

    // 카트 아이템 상세보기 (View 렌더링 용)
    @GetMapping("/detail/{cartItemId}")
    public CartItem getCartItemDetail(@PathVariable Long cartItemId) {
        return cartService.getAllCartItemInfo(cartItemId);
    }
}
