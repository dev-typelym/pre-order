package com.app.preorder.controller.cart;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.memberDTO.MemberDTO;
import com.app.preorder.domain.productDTO.ProductListDTO;
import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.member.Member;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.service.cart.CartService;
import com.app.preorder.type.CatergoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/cart/*")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final MemberRepository memberRepository;

    // 카트에 아이템 추가
    @PostMapping("cartItem/add/{productId}")
    @ResponseBody
    public void addItemToCart(@PathVariable Long productId, @RequestParam Long count) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);
        Long memberId = member.getId();
        cartService.addItem(memberId, productId, count);
    }

    // 카트에 아이템 감소
    @PostMapping("cartItem/decrease/{productId}")
    public void decreaseItemToCart(@PathVariable Long productId, @RequestParam Long count) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);
        Long memberId = member.getId();

        cartService.decreaseItem(memberId, productId, count);
    }

    // 카트에 아이템 삭제
    @PostMapping("cartItem/delete")
    @ResponseBody
    public void deleteItemFromCart(@RequestParam("checkedIds[]") List<String> checkIds) {
        cartService.deleteItem(checkIds);
    }

    // 카트 목록 페이징 조회
    @GetMapping("cartItemList/{page}")
    @ResponseBody
    public Page<CartItemListDTO> getParentsBoard(@PathVariable("page") int page){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        Long sessionId = null;

        if(member != null){
            sessionId = member.getId();
        }

        Page<CartItemListDTO> cartItemList = cartService.getCartItemListWithPaging(page -1, sessionId);
        return cartItemList;
    }

    // 카트 아이템 상세보기
    @GetMapping("detail/{cartItemId}")
    public String goCartItemDetail(@PathVariable Long cartItemId, Model model){

        CartItem CartItem = cartService.getAllCartItemInfo(cartItemId);
        model.addAttribute("CartItem", CartItem);
        return "cartItemList/detail";
    }
}
