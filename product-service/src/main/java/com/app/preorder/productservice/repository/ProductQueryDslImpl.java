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

import java.time.LocalDateTime;
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

        LocalDateTime now = LocalDateTime.now();
        // 오픈 시간 조건(시작 <= now, 종료 >= now; null 허용)
        BooleanExpression openStartCondition = product.salesPeriod.startAt.isNull()
                .or(product.salesPeriod.startAt.loe(now));
        BooleanExpression openEndCondition = product.salesPeriod.endAt.isNull()
                .or(product.salesPeriod.endAt.goe(now));

        List<Product> content = query.selectFrom(product)
                .where(nameCondition, categoryCondition, statusCondition, openStartCondition, openEndCondition)
                .orderBy(product.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(product.count())
                .from(product)
                .where(nameCondition, categoryCondition, statusCondition, openStartCondition, openEndCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, count != null ? count : 0);
    }


    //    싱픔 상세보기
    @Override
    public Optional<Product> findDetailById(Long productId) {
        LocalDateTime now = LocalDateTime.now();

        BooleanExpression openStart = product.salesPeriod.startAt.isNull()
                .or(product.salesPeriod.startAt.loe(now));
        BooleanExpression openEnd = product.salesPeriod.endAt.isNull()
                .or(product.salesPeriod.endAt.goe(now));

        Product result = query.selectFrom(product)
                .where(product.id.eq(productId), openStart, openEnd)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    @Transactional
    public int updateStatus(Long productId, ProductStatus target) {
        long updated = query.update(product)
                .set(product.status, target)
                .where(
                        product.id.eq(productId)
                                .and(product.status.ne(target))
                )
                .execute();
        em.clear();
        return (int) updated;
    }
}
