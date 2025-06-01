package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long>, StockQueryDsl{
}
