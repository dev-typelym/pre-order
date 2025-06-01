package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductQueryDsl{
}
