package com.app.preorder.productservice.repository;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.domain.entity.QProduct;
import com.app.preorder.productservice.dto.product.ProductSearchRequest;
import com.app.preorder.productservice.domain.entity.Product;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class ProductQueryDslImpl implements ProductQueryDsl{

    private final JPAQueryFactory query;
    private final QProduct product = QProduct.product;
    private final EntityManager em;

    // 상품 목록
    @Override
    public Page<Product> findAllBySearchConditions(Pageable pageable, ProductSearchRequest searchRequest, CategoryType categoryType) {
        BooleanExpression nameCondition = (searchRequest.getProductName() != null)
                ? product.productName.containsIgnoreCase(searchRequest.getProductName())
                : null;

        BooleanExpression categoryCondition = (categoryType != null)
                ? product.category.eq(categoryType)
                : null;

        BooleanExpression statusCondition = product.status.eq(ProductStatus.ENABLED);

        List<Product> content = query.selectFrom(product)
                .where(nameCondition, categoryCondition, statusCondition)
                .orderBy(product.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(product.count())
                .from(product)
                .where(nameCondition, categoryCondition, statusCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, count != null ? count : 0);
    }


    //    싱픔 상세보기
    @Override
    public Optional<Product> findByIdWithStocks(Long productId) {
        Product result = query.selectFrom(product)
                .leftJoin(product.stocks).fetchJoin()
                .where(product.id.eq(productId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    @Transactional
    public int updateStatus(Long productId, ProductStatus status) {
        long updated = query.update(product)
                .set(product.status, status)
                .where(product.id.eq(productId))
                .execute();
        em.clear(); // ✔ 벌크 연산은 영속성 컨텍스트와 분리되므로 클리어 권장
        return (int) updated;
    }
}
