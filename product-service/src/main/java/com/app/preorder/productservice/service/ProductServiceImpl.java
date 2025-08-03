package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.PendingQuantityInternal;
import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import com.app.preorder.common.exception.custom.StockNotFoundException;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.client.OrderServiceClient;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.dto.product.*;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.ProductRepository;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Transactional
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ProductFactory productFactory;
    private final OrderServiceClient orderClient;

    //  상품 등록
    @Override
    public Long createProduct(ProductCreateRequest request) {
        Product product = productFactory.createFrom(request);
        productRepository.save(product);
        return product.getId();
    }

    //  상품 수정
    @Override
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));
        productFactory.updateFrom(request, product);
    }

    //  상품 삭제
    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));
        productRepository.delete(product);
    }

    //  상품 목록
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType) {
        Page<Product> products = productRepository.findAllBySearchConditions(PageRequest.of(page, 5), searchRequest, categoryType);
        List<Product> content = products.getContent();
        List<Long> productIds = content.stream().map(Product::getId).toList();

        // ✅ (1) pending 수량 미리 조회
        Map<Long, Long> pendingMap = orderClient.getPendingQuantities(productIds).stream()
                .collect(Collectors.toMap(PendingQuantityInternal::getProductId, PendingQuantityInternal::getQuantity));

        // ✅ (2) 재고 수량도 미리 조회 (N+1 제거 핵심)
        Map<Long, Long> stockMap = stockRepository.findByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        stock -> stock.getProduct().getId(),
                        stock -> stock.getStockQuantity()
                ));

        // ✅ (3) 각 상품에 대해 가용 수량 계산 → 응답 변환
        List<ProductResponse> responses = content.stream()
                .map(product -> {
                    long stock = stockMap.getOrDefault(product.getId(), 0L);
                    long pending = pendingMap.getOrDefault(product.getId(), 0L);
                    long available = Math.max(stock - pending, 0L);
                    return productFactory.toResponse(product, available);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, products.getPageable(), products.getTotalElements());
    }

    //  상품 상세
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithStocks(productId)
                .orElseThrow(() -> new ProductNotFoundException("해당 상품을 찾을 수 없습니다."));

        long stock = product.getStocks().get(0).getStockQuantity();
        long pending = orderClient.getPendingQuantities(List.of(productId)).stream()
                .filter(p -> p.getProductId().equals(productId))
                .findFirst()
                .map(PendingQuantityInternal::getQuantity)
                .orElse(0L);

        return productFactory.toResponse(product, stock - pending);
    }

    //  상품 단건 조회(feign)
    @Override
    @Transactional(readOnly = true)
    public ProductInternal getProductById(Long productId) {
        Product product = productRepository.findById(productId) // ✅ 명확하고 일관됨
                .orElseThrow(() -> new ProductNotFoundException("해당 상품을 찾을 수 없습니다."));
        return productFactory.toProductInternal(product);
    }

    //  상품 다건 조회(feign)
    @Override
    @Transactional(readOnly = true)
    public List<ProductInternal> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ProductNotFoundException("일부 상품을 찾을 수 없습니다.");
        }
        return products.stream()
                .map(productFactory::toProductInternal)
                .collect(Collectors.toList());
    }


    // 상품 ID 목록으로 가용 재고 수량 계산 (재고 - 결제대기수량)
    @Override
    public List<ProductAvailableStockResponse> getAvailableQuantities(List<Long> productIds) {
        Map<Long, Long> pendingMap = orderClient.getPendingQuantities(productIds).stream()
                .collect(Collectors.toMap(
                        PendingQuantityInternal::getProductId,
                        PendingQuantityInternal::getQuantity  // ✅ 여기 수정
                ));

        return stockRepository.findByProductIds(productIds).stream()
                .map(stock -> {
                    Long pending = pendingMap.getOrDefault(stock.getProduct().getId(), 0L);
                    Long available = stock.getStockQuantity() - pending;
                    return new ProductAvailableStockResponse(stock.getProduct().getId(), Math.max(available, 0L));
                }).collect(Collectors.toList());
    }
}
