package com.app.preorder.service.order;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.orderDTO.OrderListDTO;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import com.app.preorder.entity.product.Product;
import com.app.preorder.entity.product.Stock;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.repository.product.ProductRepository;
import com.app.preorder.repository.product.StockRepository;
import com.app.preorder.type.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private StockRepository stockRepository;

    // 단건 주문
    @Override
    public void addOrder(Long memberId, Long productId, Long quantity) {
        Member member = memberRepository.findMemberById(memberId);
        Product product = productRepository.findProductByProductId_queryDSL(productId);
        OrderItem orderItem = OrderItem.builder().quantity(quantity).product(product).regDate(LocalDateTime.now()).build();
        Order order = Order.builder().orderDate(LocalDateTime.now()).member(member).status(OrderStatus.ORDER_COMPLETE).orderPrice(orderItem.getProduct().getProductPrice()).build();
        order.addOrderItem(orderItem);
        Stock stock = stockRepository.findStockByProductId_queryDSL(productId);
        stock.updateStockQuantity(stock.getStockQuantity() - quantity);
        orderRepository.save(order);

    }


    // 카트 다건 주문
    @Override
    public void addOrderFromCart(Long memberId, List<String> productIds, List<String> quantities) {
        Member member = memberRepository.findMemberById(memberId);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalPrice = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderDate(now)
                .member(member)
                .status(OrderStatus.ORDER_COMPLETE)
                .build();

        Map<String, Long> productIdToQuantityMap = new HashMap<>();

        // 상품별 수량을 맵에 저장
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            Long quantity = Long.parseLong(quantities.get(i));
            productIdToQuantityMap.put(productId, productIdToQuantityMap.getOrDefault(productId, 0L) + quantity);
        }

        for (Map.Entry<String, Long> entry : productIdToQuantityMap.entrySet()) {
            String productId = entry.getKey();
            Long productPerQuantity = entry.getValue();

            Product product = productRepository.findProductByProductId_queryDSL(Long.parseLong(productId));

            OrderItem orderItem = OrderItem.builder()
                    .quantity(productPerQuantity)
                    .product(product)
                    .regDate(now)
                    .build();

            order.addOrderItem(orderItem);
            totalPrice = totalPrice.add(orderItem.getProduct().getProductPrice().multiply(BigDecimal.valueOf(productPerQuantity)));

            // 재고 업데이트
            Stock stock = stockRepository.findStockByProductId_queryDSL(Long.parseLong(productId));
            stock.updateStockQuantity(stock.getStockQuantity() - productPerQuantity);

        }

        order.updateOrderPrice(totalPrice);
        orderRepository.save(order);
    }

    // 주문 목록
    @Override
    public Page<OrderListDTO> getOrderListWithPaging(int page, Long memberId) {
        Page<Order> orders = orderRepository.findAllOrder_queryDSL(PageRequest.of(page, 5), memberId);
        List<OrderListDTO> orderListDTOS = orders.getContent().stream()
                .map(this::toOrderListDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(orderListDTOS, orders.getPageable(), orders.getTotalElements());
    }

    // 주문 상세보기
    @Override
    public OrderListDTO getOrderItemsInOrder(Long orderId){
        Order orderItems = orderRepository.findOrderItemsByOrderId_queryDSL(orderId);
        OrderListDTO orderItemListDTO = toOrderListDTO(orderItems);
        return orderItemListDTO;
    }

//    // 주문 취소
//    @Override
//    public void orderCancel(Long orderId) {
//        Order order = orderRepository.findOrderById(orderId);
//    }
}
