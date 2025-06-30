    package com.app.preorder.orderservice.service;

    import com.app.preorder.common.dto.ProductInternal;
    import com.app.preorder.common.dto.StockRequestInternal;
    import com.app.preorder.common.dto.StockInternal;
    import com.app.preorder.common.exception.custom.*;
    import com.app.preorder.common.type.OrderStatus;
    import com.app.preorder.orderservice.client.ProductServiceClient;
    import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
    import com.app.preorder.orderservice.domain.order.OrderItemRequest;
    import com.app.preorder.orderservice.domain.order.OrderResponse;
    import com.app.preorder.orderservice.entity.Order;
    import com.app.preorder.orderservice.entity.OrderItem;
    import com.app.preorder.orderservice.factory.OrderFactory;
    import com.app.preorder.orderservice.repository.OrderRepository;
    import com.app.preorder.orderservice.scheduler.OrderScheduler;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageImpl;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.stereotype.Service;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Slf4j
    @RequiredArgsConstructor
    @Service
    public class OrderServiceImpl implements OrderService {

        private final ProductServiceClient productClient;
        private final OrderRepository orderRepository;
        private final OrderFactory orderFactory;
        private final OrderScheduler orderScheduler;

        // 단건 주문
        @Override
        @Transactional
        public Long orderSingleItem(Long memberId, Long productId, Long quantity) {
            ProductInternal product;
            StockInternal stock;

            try {
                product = productClient.getProductsByIds(List.of(productId)).get(0);
                stock = productClient.getStocksByIds(List.of(productId)).get(0);
            } catch (feign.FeignException ex) {
                throw new FeignException("상품 서비스 통신 오류", ex);
            }

            if (stock.getStockQuantity() < quantity) {
                throw new InsufficientStockException(
                        "상품 ID [" + productId + "]의 재고가 부족합니다. 요청 수량: " + quantity + ", 보유 재고: " + stock.getStockQuantity()
                );
            }

            OrderItem item = orderFactory.createOrderItem(product, quantity);
            Order order = orderFactory.createOrder(memberId, item);

            orderRepository.save(order);
            orderScheduler.scheduleAll(order.getId());

            return order.getId();
        }



        // 카트 다건 주문
        @Override
        @Transactional
        public Long orderFromCart(Long memberId, List<OrderItemRequest> items) {

            // [1] 주문할 상품 ID와 수량을 Map 형태로 변환
            Map<Long, Long> quantityMap = items.stream()
                    .collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity));

            // [2] 상품 ID 목록 추출
            List<Long> productIds = new ArrayList<>(quantityMap.keySet());

            List<ProductInternal> products;

            try {
                // [3] 상품 정보 조회 (FeignClient - product-service)
                products = productClient.getProductsByIds(productIds);
            } catch (FeignException e) {
                log.error("상품 서비스 조회 실패", e);
                throw new FeignException("상품 서비스 조회 실패", e);
            }

            // [4] 재고 차감 요청 (FeignClient - product-service)
            List<StockRequestInternal> deductList = items.stream()
                    .map(i -> new StockRequestInternal(i.getProductId(), i.getQuantity()))
                    .toList();

            try {
                productClient.deductStocks(deductList);
            } catch (FeignException e) {
                log.error("상품 서비스 재고 차감 실패", e);
                throw new FeignException("상품 서비스 재고 차감 실패", e);
            }

            // [5] 주문 엔티티 생성 (Factory 사용)
            Order order = orderFactory.createOrderFromCart(memberId, products, quantityMap);

            // [6] 주문 저장
            orderRepository.save(order);

            // [7] 주문 ID 반환
            return order.getId();
        }

        // 주문 목록
        @Override
        public Page<OrderResponse> getOrdersWithPaging(int page, Long memberId) {
            int pageSize = 10; // 고정값
            PageRequest pageable = PageRequest.of(page, pageSize);

            Page<Order> orders = orderRepository.findOrdersByMemberId(memberId, pageable);

            List<OrderResponse> responseList = orders.getContent().stream()
                    .map(orderFactory::toOrderResponse)
                    .toList();

            return new PageImpl<>(responseList, pageable, orders.getTotalElements());
        }

        // 주문 상세보기
        @Override
        public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
            Order order = orderRepository.findOrderItemsById(orderId);

            if (order == null) {
                throw new OrderNotFoundException("주문을 찾을 수 없습니다.");
            }

            if (!order.getMemberId().equals(memberId)) {
                throw new ForbiddenException("본인의 주문만 조회할 수 있습니다.");
            }

            return orderFactory.toOrderDetailResponse(order);
        }


        // 주문 취소
        @Override
        public void orderCancel(Long orderId) {
            Order order = findOrder(orderId);

            if (!OrderStatus.ORDER_COMPLETE.equals(order.getStatus())) {
                throw new InvalidOrderStatusException("완료된 주문만 취소할 수 있습니다.");
            }

            // 주문 상태 업데이트
            order.updateOrderStatus(OrderStatus.ORDER_CANCEL);

            // 주문 아이템 -> 재고 복원 목록 준비
            List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                    .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                    .toList();

            // StockService Feign 호출
            try {
                productClient.restoreStocks(restoreItems);
            } catch (FeignException e) {
                order.updateOrderStatus(OrderStatus.ORDER_COMPLETE);
                throw new FeignException("재고 복원 실패, 주문 상태를 복구했습니다.", e);
            }
        }

        // 반품 신청
        @Override
        public void orderReturn(Long orderId) {

            Order order = findOrder(orderId);

            if (!OrderStatus.DELIVERY_COMPLETE.equals(order.getStatus())) {
                throw new InvalidOrderStatusException("배송 완료된 주문만 반품 신청할 수 있습니다.");
            }

            order.updateOrderStatus(OrderStatus.RETURNING);

            orderScheduler.scheduleReturnProcess(orderId);
        }

        // 주문 상태를 "배송 중"으로 업데이트
        @Override
        public void updateOrderStatusShipping(Long orderId) {
            Order order = findOrder(orderId);
            if (order.getStatus() == OrderStatus.ORDER_COMPLETE) {
                order.updateOrderStatus(OrderStatus.DELIVERYING);
            }
        }

        // 주문 상태를 "배송 완료"로 업데이트
        @Override
        public void updateOrderStatusDelivered(Long orderId) {
            Order order = findOrder(orderId);
            if (order.getStatus() == OrderStatus.DELIVERYING) {
                order.updateOrderStatus(OrderStatus.DELIVERY_COMPLETE);
            }
        }

        // 주문 상태를 "반품 불가"로 업데이트
        @Override
        public void updateOrderStatusNonReturnable(Long orderId) {
            Order order = findOrder(orderId);
            if (order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
                order.updateOrderStatus(OrderStatus.RETURN_NOT_PERMITTED);
            }
        }

        // 반품 처리 (재고 복원 포함)
        @Override
        public void processReturn(Long orderId) {
            Order order = findOrder(orderId);
            if (order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
                order.updateOrderStatus(OrderStatus.RETURN_COMPLETE);

                List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                        .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                        .toList();

                try {
                    productClient.restoreStocks(restoreItems);
                } catch (FeignException e) {
                    // 💡 보상 로직
                    order.updateOrderStatus(OrderStatus.DELIVERY_COMPLETE);
                    throw new FeignException("재고 복원 실패, 주문 상태를 복구했습니다.", e);
                }
            }
        }

        private Order findOrder(Long orderId) {
            return orderRepository.findOrderById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
        }

    }
