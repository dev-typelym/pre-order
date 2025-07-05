package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.common.exception.custom.StockNotFoundException;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductFactory productFactory;

    @Override
    @Transactional(readOnly = true)
    public List<StockInternal> getStocksByIds(List<Long> productIds) {
        List<Stock> stocks = stockRepository.findStocksByIds(productIds);
        return stocks.stream()
                .map(productFactory::toStockInternal)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deductStocks(List<StockRequestInternal> items) {
        // 1. productIds 추출
        List<Long> productIds = items.stream()
                .map(StockRequestInternal::getProductId)
                .toList();

        // 2. 다건 조회
        List<Stock> stocks = stockRepository.findStocksByIds(productIds);

        // 3. Map 변환
        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), stock -> stock));

        // 4. 검증 및 차감
        for (StockRequestInternal item : items) {
            Stock stock = stockMap.get(item.getProductId());
            if (stock == null) {
                throw new StockNotFoundException("해당 상품의 재고를 찾을 수 없습니다.");
            }
            if (stock.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("상품 ID [" + item.getProductId() + "]의 재고가 부족합니다.");
            }

            stock.decrease(item.getQuantity());

            //  재고가 0이면 상품 상태 SOLD_OUT 처리
            if (stock.getStockQuantity() == 0) {
                stock.getProduct().updateStatus(ProductStatus.SOLD_OUT);
            }
        }
    }

    @Override
    @Transactional
    public void restoreStocks(List<StockRequestInternal> items) {
        List<Long> productIds = items.stream()
                .map(StockRequestInternal::getProductId)
                .toList();

        List<Stock> stocks = stockRepository.findStocksByIds(productIds);

        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), stock -> stock));

        for (StockRequestInternal item : items) {
            Stock stock = stockMap.get(item.getProductId());

            if (stock == null) {
                throw new StockNotFoundException("해당 상품 ID [" + item.getProductId() + "]에 대한 재고 정보를 찾을 수 없습니다.");
            }

            stock.updateStockQuantity(stock.getStockQuantity() + item.getQuantity());

            //  재고가 0에서 증가하면 ENABLED로 변경
            if (stock.getStockQuantity() > 0) {
                stock.getProduct().updateStatus(ProductStatus.ENABLED);
            }
        }
    }
}