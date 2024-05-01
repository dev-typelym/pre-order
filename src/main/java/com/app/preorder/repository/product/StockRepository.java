package com.app.preorder.repository.product;

import com.app.preorder.entity.product.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long>, StockQueryDsl{
}
