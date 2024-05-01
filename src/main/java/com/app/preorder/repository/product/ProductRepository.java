package com.app.preorder.repository.product;

import com.app.preorder.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductQueryDsl{
}
