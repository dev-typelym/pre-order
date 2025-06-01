package com.app.preorder.productservice.repository;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductListSearch;
import com.app.preorder.productservice.domain.entity.Product;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
public class ProductQueryDslImpl implements ProductQueryDsl{

    private final JPAQueryFactory query;
    private final QProduct product = QProduct.product;

    // 상품아이디로 상품조회
    @Override
    public Product findProductByProductId_queryDSL(Long productId){
        return query.selectFrom(product)
                .where(product.id.eq(productId))
                .fetchOne();
    }

    // 상품 목록
    @Override
    public Page<Product> findAllProduct_queryDSL(Pageable pageable, ProductListSearch productListSearch, CategoryType productCategory) {
        BooleanExpression productNameEq = productListSearch.getProductName() == null ? null : product.productName.like("%" + productListSearch.getProductName()    + "%");

        List<Product> foundAnnouncement = query.select(product)
                .from(product)
                .where((productCategory != null ? product.category.eq(productCategory) : product.category.isNotNull()).and(productNameEq))
                .orderBy(product.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(product.count())
                .from(product)
                .where(productNameEq)
                .fetchOne();

        return new PageImpl<>(foundAnnouncement, pageable, count);
    }

    //    싱픔 상세보기
    @Override
    public List<Product> findAllProductDetail_queryDSL() {
        List<Product> foundProductDetail =
                query.select(product)
                        .from(product)
                        .leftJoin(product.stocks)
                        .fetchJoin()
                        .fetch();
        return foundProductDetail;
    }
}
