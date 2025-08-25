package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;

import java.util.List;
import java.util.Optional;

public interface StockQueryDsl {

    // 상품 ID 목록으로 재고 조회 (다건)
    List<Stock> findByProductIds(List<Long> productIds);

    // ✅ 원자 업데이트 3종
    int reserve(long productId, long qty);    // reserved += qty (가능할 때만)
    int unreserve(long productId, long qty);  // reserved -= qty (음수 방지)
    int restock(long productId, long qty);
    int commit(long productId, long qty);
    // ✅ 가용 재고 조회: quantity - reserved
    Optional<Long> findAvailable(long productId);
}
