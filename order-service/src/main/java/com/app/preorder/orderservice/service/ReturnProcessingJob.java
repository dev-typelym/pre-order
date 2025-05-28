package com.app.preorder.orderservice.service;

import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import com.app.preorder.entity.product.Product;
import com.app.preorder.entity.product.Stock;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.repository.product.StockRepository;
import com.app.preorder.type.OrderStatus;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReturnProcessingJob implements Job {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private StockRepository stockRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
            // 반품 처리 로직
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                Long quantity = item.getQuantity();

                Stock stock = stockRepository.findStockByProductId_queryDSL(product.getId());
                if (stock != null) {
                    stock.updateStockQuantity(stock.getStockQuantity() + quantity);
                } else {
                    throw new IllegalStateException("No stock record found for product ID: " + product.getId());
                }
            }
            order.updateOrderStatus(OrderStatus.RETURN_COMPLETE);
            orderRepository.save(order);
        }
    }
}
