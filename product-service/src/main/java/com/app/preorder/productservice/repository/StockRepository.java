package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long>, StockQueryDsl{

    @Query("SELECT s FROM Stock s WHERE s.product.id IN :productIds")
    List<Stock> findStocksByIds(@Param("productIds") List<Long> productIds);
}
