package com.app.preorder.repository.product;

import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.entity.product.Product;
import com.app.preorder.entity.product.QProduct;
import com.app.preorder.type.CatergoryType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.app.preorder.entity.product.QProduct.product;

@RequiredArgsConstructor
public class ProductQueryDslImpl implements ProductQueryDsl{

    private final JPAQueryFactory query;

    // 상품아이디로 상품조회
    @Override
    public Product findProductByProductId_queryDSL(Long productId){
        return query.selectFrom(product)
                .where(product.id.eq(productId))
                .fetchOne();
    }

    // 상품 목록
    @Override
    public Page<Product> findAllProduct_queryDSL(Pageable pageable, ProductListSearch productListSearch, CatergoryType productCategory) {
        BooleanExpression productNameEq = productListSearch.getProductName() == null ? null : product.productName.like("%" + productListSearch.getProductName()    + "%");

        QProduct product = QProduct.product;
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
