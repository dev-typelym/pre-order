package com.app.preorder.service.order;

import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import com.app.preorder.entity.product.Product;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.repository.product.ProductRepository;
import com.app.preorder.type.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderServiceImpl(MemberRepository memberRepository, ProductRepository productRepository, OrderRepository orderRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    // 단건 주문
    @Override
    public void addOrder(Long memberId, Long productId, Long quantity) {
        Member member = memberRepository.findMemberById(memberId);
        Product product = productRepository.findProductByProductId_queryDSL(productId);
        OrderItem orderItem = OrderItem.builder().quantity(quantity).product(product).regDate(LocalDateTime.now()).build();
        Order order = Order.builder().orderDate(LocalDateTime.now()).member(member).status(OrderStatus.ORDER_COMPLETE).orderPrice(orderItem.getProduct().getProductPrice()).build();
        order.addOrderItem(orderItem);
        orderRepository.save(order);

    }
    // 카트 다건 주문
}
