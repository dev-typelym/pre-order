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
    import com.app.preorder.orderservice.factory.OrderFactory;
    import com.app.preorder.orderservice.repository.OrderRepository;
    import com.app.preorder.orderservice.scheduler.OrderScheduler;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageImpl;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

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
        private final ProductServiceClient productServiceClient;
        private final OrderTransactionalService orderTransactionalService; // ✅ 추가

        // 단건 주문
        @Override
        public Long orderSingleItem(Long memberId, Long productId, Long quantity) {
            ProductInternal product;
            StockInternal stock;

            try {
                product = productClient.getProductsByIds(List.of(productId)).get(0);
                stock = productClient.getStocksByIds(List.of(productId)).get(0);
            } catch (feign.FeignException ex) {
                throw new FeignException("상품 서비스 통신 오류", ex);
            }

            // ✅ 상품 상태 체크
            if (!product.getStatus().name().equals("ENABLED")) {
                throw new InvalidProductStatusException("상품이 판매 가능 상태가 아닙니다.");
            }

            // 기존 재고 체크
            if (stock.getStockQuantity() < quantity) {
                throw new InsufficientStockException(
                        "상품 ID [" + productId + "]의 재고가 부족합니다. 요청 수량: " + quantity + ", 보유 재고: " + stock.getStockQuantity()
                );
            }

            try {
                List<StockRequestInternal> stockList = List.of(new StockRequestInternal(productId, quantity));
                productServiceClient.deductStocks(stockList);
            } catch (feign.FeignException ex) {
                throw new FeignException("상품 재고 차감 실패", ex);
            }

            try {
                return orderTransactionalService.saveOrderInTransaction(memberId, product, quantity);
            } catch (Exception e) {
                try {
                    List<StockRequestInternal> stockList = List.of(new StockRequestInternal(productId, quantity));
                    productServiceClient.restoreStocks(stockList);
                } catch (feign.FeignException restoreEx) {
                    log.error("보상 트랜잭션(재고 복원) 실패 - productId: {}, quantity: {}", productId, quantity, restoreEx);
                }
                throw e;
            }
        }

        // 카트 다건 주문
        @Override
        public Long orderFromCart(Long memberId, List<OrderItemRequest> items) {

            Map<Long, Long> quantityMap = items.stream()
                    .collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity));

            List<Long> productIds = new ArrayList<>(quantityMap.keySet());
            List<ProductInternal> products;

            //  상품 정보 조회
            try {
                products = productClient.getProductsByIds(productIds);
            } catch (FeignException e) {
                throw new FeignException("상품 서비스 조회 실패", e);
            }

            //  상품 상태 체크
            for (ProductInternal p : products) {
                if (!p.getStatus().name().equals("ENABLED")) {
                    throw new InvalidProductStatusException("상품이 판매 가능 상태가 아닙니다. id: " + p.getId());
                }
            }

            //  재고 차감
            List<StockRequestInternal> deductList = items.stream()
                    .map(i -> new StockRequestInternal(i.getProductId(), i.getQuantity()))
                    .toList();

            try {
                productClient.deductStocks(deductList);
            } catch (FeignException e) {
                throw new FeignException("상품 재고 차감 실패", e);
            }

            //  DB 트랜잭션
            try {
                return orderTransactionalService.saveOrderFromCartInTransaction(memberId, products, quantityMap);
            } catch (Exception e) {
                // Step 4: 보상 트랜잭션 (재고 복원)
                try {
                    productClient.restoreStocks(deductList);
                } catch (FeignException restoreEx) {
                    log.error("보상 트랜잭션(재고 복원) 실패 - productIds: {}", productIds, restoreEx);
                }
                throw e;
            }
        }

        // 주문 목록
        @Override
        @Transactional(readOnly  = true)
        public Page<OrderResponse> getOrdersWithPaging(int page, int size, Long memberId) {
            PageRequest pageable = PageRequest.of(page, size);
            Page<Order> orders = orderRepository.findOrdersByMemberId(memberId, pageable);

            List<OrderResponse> responseList = orders.getContent().stream()
                    .map(orderFactory::toOrderResponse)
                    .toList();

            return new PageImpl<>(responseList, pageable, orders.getTotalElements());
        }

        // 주문 상세보기
        @Override
        @Transactional(readOnly = true)
        public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
            Order order = orderRepository.findOrderItemsById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));

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

            // Step 1: 재고 복원
            List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                    .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                    .toList();

            try {
                productClient.restoreStocks(restoreItems);
            } catch (FeignException e) {
                throw new FeignException("재고 복원 실패", e);
            }

            // Step 2: DB 트랜잭션 - 상태 변경
            try {
                orderTransactionalService.cancelOrderInTransaction(order);
            } catch (Exception e) {
                // Step 3: 보상 트랜잭션 - 재차감
                try {
                    productClient.deductStocks(restoreItems);
                } catch (FeignException restoreEx) {
                    log.error("보상 트랜잭션(재차감) 실패 - orderId: {}", orderId, restoreEx);
                }
                throw e;
            }
        }

        // 반품 신청
        @Override
        public void orderReturn(Long orderId) {
            Order order = findOrder(orderId);

            if (!OrderStatus.DELIVERY_COMPLETE.equals(order.getStatus())) {
                throw new InvalidOrderStatusException("배송 완료된 주문만 반품 신청할 수 있습니다.");
            }

            // Step 1: 스케줄 등록
            try {
                orderScheduler.scheduleReturnProcess(orderId);
            } catch (Exception e) {
                throw new RuntimeException("반품 스케줄 등록 실패", e);
            }

            // Step 2: DB 트랜잭션 (상태 변경)
            try {
                orderTransactionalService.updateOrderStatusToReturning(order);
            } catch (Exception e) {
                // Step 3: 보상 (스케줄 취소)
                try {
                    orderScheduler.cancelReturnProcess(orderId);
                } catch (Exception cancelEx) {
                    log.error("보상 트랜잭션(스케줄 취소) 실패 - orderId: {}", orderId, cancelEx);
                }
                throw e;
            }
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

            if (order.getStatus() != OrderStatus.DELIVERY_COMPLETE) {
                throw new InvalidOrderStatusException("배송 완료 상태의 주문만 반품 처리할 수 있습니다.");
            }

            // Step 1: 재고 복원
            List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                    .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                    .toList();

            try {
                productClient.restoreStocks(restoreItems);
            } catch (FeignException e) {
                throw new FeignException("재고 복원 실패", e);
            }

            // Step 2: DB 트랜잭션
            try {
                orderTransactionalService.updateOrderStatusToReturnComplete(order);
            } catch (Exception e) {
                // Step 3: 보상 (재차감)
                try {
                    productClient.deductStocks(restoreItems);
                } catch (FeignException restoreEx) {
                    log.error("보상 트랜잭션(재차감) 실패 - orderId: {}", orderId, restoreEx);
                }
                throw e;
            }
        }

        private Order findOrder(Long orderId) {
            return orderRepository.findOrderById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
        }

    }
