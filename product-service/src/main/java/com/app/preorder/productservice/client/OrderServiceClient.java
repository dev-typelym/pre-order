package com.app.preorder.productservice.client;

import com.app.preorder.common.dto.PendingQuantityInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "order-service", path = "/api/internal/orders")
public interface OrderServiceClient {

    @PostMapping("/pending-quantities")
    List<PendingQuantityInternal> getPendingQuantities(@RequestBody List<Long> productIds);
}
