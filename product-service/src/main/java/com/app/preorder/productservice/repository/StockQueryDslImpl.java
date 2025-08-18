package com.app.preorder.productservice.repository;

import com.app.preorder.productservice.domain.entity.QStock;
import com.app.preorder.productservice.domain.entity.Stock;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class StockQueryDslImpl implements StockQueryDsl {

    private final JPAQueryFactory query;
    private final EntityManager em;

    private static final QStock s = QStock.stock;

    // 상품 ID 목록으로 재고 조회 (다건)
    @Override
    public List<Stock> findByProductIds(List<Long> productIds) {
        QStock stock = QStock.stock;

        return query.selectFrom(stock)
                .where(stock.product.id.in(productIds))
                .fetch();
    }

    // ------------ ✅ 원자 UPDATE 3종 ------------

    @Override
    @Transactional
    public int reserve(long pid, long q) {
        long updated = query.update(s)
                .set(s.reserved, s.reserved.add(q))
                .where(
                        s.product.id.eq(pid)
                                .and(s.stockQuantity.subtract(s.reserved).goe(q))
                )
                .execute();
        em.clear(); // 중요: 벌크 후 1차 캐시 갱신
        return (int) updated; // 1=성공, 0=재고부족
    }

    @Override
    @Transactional
    public int unreserve(long pid, long q) {
        long updated = query.update(s)
                .set(s.reserved, s.reserved.subtract(q))
                .where(
                        s.product.id.eq(pid)
                                .and(s.reserved.goe(q))
                )
                .execute();
        em.clear();
        return (int) updated; // 1=성공, 0=조건불충족
    }

    @Override
    @Transactional
    public int consumeReserved(long pid, long q) {
        long updated = query.update(s)
                .set(s.stockQuantity, s.stockQuantity.subtract(q))
                .set(s.reserved,     s.reserved.subtract(q))
                .where(
                        s.product.id.eq(pid)
                                .and(s.stockQuantity.goe(q))
                                .and(s.reserved.goe(q))
                )
                .execute();
        em.clear();
        return (int) updated; // 1=성공, 0=조건불충족
    }

    @Override
    @Transactional
    public int restock(long pid, long q) {
        long updated = query.update(s)
                .set(s.stockQuantity, s.stockQuantity.add(q))
                .where(s.product.id.eq(pid))
                .execute();
        em.clear();
        return (int) updated; // 1=성공
    }

    // ------------ ✅ 가용 재고 조회 ------------

    @Override
    public Optional<Long> findAvailable(long pid) {
        Long v = query
                .select(s.stockQuantity.subtract(s.reserved))
                .from(s)
                .where(s.product.id.eq(pid))
                .fetchOne();
        return Optional.ofNullable(v);
    }

}

