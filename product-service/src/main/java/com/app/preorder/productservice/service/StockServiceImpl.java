package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.StockNotFoundException;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductFactory productFactory;

    @Override
    public StockInternal getStockById(Long productId) {
        Stock stock = stockRepository.findStockById(productId)
                .orElseThrow(() -> new StockNotFoundException("해당 상품의 재고를 찾을 수 없습니다."));
        return productFactory.toStockInternal(stock);
    }
}