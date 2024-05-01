package com.app.preorder.service.order;

import com.app.preorder.domain.orderDTO.OrderListDTO;
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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
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
    @Autowired
    private Scheduler scheduler;

    // 단건 주문
    @Override
    @Transactional
    public Long addOrder(Long memberId, Long productId, Long quantity) {
        Member member = memberRepository.findMemberById(memberId);
        Product product = productRepository.findProductByProductId_queryDSL(productId);
        OrderItem orderItem = OrderItem.builder().quantity(quantity).product(product).regDate(LocalDateTime.now()).build();
        Order order = Order.builder().orderDate(LocalDateTime.now()).member(member).status(OrderStatus.ORDER_COMPLETE).orderPrice(orderItem.getProduct().getProductPrice()).build();
        order.addOrderItem(orderItem);
        Stock stock = stockRepository.findStockByProductId_queryDSL(productId);
        stock.updateStockQuantity(stock.getStockQuantity() - quantity);
        orderRepository.save(order);
        return order.getId();
    }


    // 카트 다건 주문
    @Override
    @Transactional
    public Long addOrderFromCart(Long memberId, List<String> productIds, List<String> quantities) {
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
        return order.getId();
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

    // 주문 취소
    @Override
    public void orderCancel(Long orderId) {
        Order order = orderRepository.findOrderByOrderId_queryDSL(orderId);

        if (order.getStatus() != OrderStatus.ORDER_COMPLETE) {
            throw new IllegalStateException("Only completed orders can be canceled.");
        }

        order.updateOrderStatus(OrderStatus.ORDER_CANCEL);

        // 주문 항목에 대해 반복하여 재고 복원
        order.getOrderItems().forEach(item -> {
            Product product = item.getProduct();
            long quantity = item.getQuantity();

            // 해당 상품의 재고를 찾아서 주문 수량만큼 재고를 복원
            Stock stock = stockRepository.findStockByProductId_queryDSL(product.getId());
            if (stock != null) {
                long updatedQuantity = stock.getStockQuantity() + quantity;
                stock.updateStockQuantity(updatedQuantity);
            } else {
                throw new IllegalStateException("No stock record found for product ID: " + product.getId());
            }
        });
    }

    // 반품 신청
    @Override
    public void orderReturn(Long orderId) {
        Order order = orderRepository.findOrderByOrderId_queryDSL(orderId);

        if (order.getStatus() != OrderStatus.DELIVERY_COMPLETE) {
            throw new IllegalStateException("Only completed orders can be canceled.");
        }

        order.updateOrderStatus(OrderStatus.RETURNING);
    }

    // 스케쥴링 init
    @PostConstruct
    public void init() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 주문상태 배달중으로 바꾸는 스케쥴링 메소드
    public void scheduleOrderShipping(Long orderId) {
        JobDetail jobDetail = JobBuilder.newJob(OrderShippingJob.class)
                .withIdentity("OrderShippingJob-" + orderId)
                .usingJobData("orderId", orderId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("OrderShippingTrigger-" + orderId)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.DAY))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 주문상태 배달중에서 배달완료로 바꾸는 스케쥴링 메소드
    public void scheduleOrderDelivered(Long orderId) {
        JobDetail jobDetail = JobBuilder.newJob(OrderDeliveredJob.class)
                .withIdentity("OrderDeliveredJob-" + orderId)
                .usingJobData("orderId", orderId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("OrderDeliveredTrigger-" + orderId)
                .startAt(DateBuilder.futureDate(2, DateBuilder.IntervalUnit.DAY))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 반품 신청후 처리를 스케줄링 하는 메소드
    public void scheduleReturnProcess(Long orderId) {
        JobDetail jobDetail = JobBuilder.newJob(ReturnProcessingJob.class)
                .withIdentity("ReturnProcessingJob-" + orderId)
                .usingJobData("orderId", orderId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("ReturnProcessingTrigger-" + orderId)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.DAY))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 배송완료후 1일 이후엔 배송상태를 반품 불가로 바꾸는 메소드
    public void scheduleNonReturnable(Long orderId) {
        JobDetail jobDetail = JobBuilder.newJob(OrderNonReturnableJob.class)
                .withIdentity("OrderNonReturnableJob-" + orderId)
                .usingJobData("orderId", orderId)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("OrderNonReturnableTrigger-" + orderId)
                .startAt(DateBuilder.futureDate(3, DateBuilder.IntervalUnit.DAY))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}
