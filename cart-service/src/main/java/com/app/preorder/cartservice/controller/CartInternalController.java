package com.app.preorder.cartservice.controller;

import com.app.preorder.cartservice.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/carts")
@RequiredArgsConstructor
public class CartInternalController {

    private final CartService cartService;

    @PostMapping
    public void createCart(@RequestBody Long memberId) {
        cartService.createCartForMember(memberId);
    }
}
