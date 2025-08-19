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

        // 재고 한 번에 조회 → availableMap 만들기
        Map<Long, Long> availableMap = stockRepository.findByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getProduct().getId(),
                        s -> Math.max((s.getStockQuantity() == null ? 0L : s.getStockQuantity())
                                - (s.getReserved() == null ? 0L : s.getReserved()), 0L)
                ));

        List<ProductResponse> responses = content.stream()
                .map(p -> productFactory.toResponse(p, availableMap.getOrDefault(p.getId(), 0L)))
                .toList();

        return new PageImpl<>(responses, products.getPageable(), products.getTotalElements());
    }

    //  상품 상세
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithStock(productId)
                .orElseThrow(() -> new ProductNotFoundException("해당 상품을 찾을 수 없습니다."));

        Stock stock = product.getStock(); // fetch join됨
        long qty = stock.getStockQuantity() == null ? 0L : stock.getStockQuantity();
        long res = stock.getReserved() == null ? 0L : stock.getReserved();
        long available = Math.max(qty - res, 0L);

        return productFactory.toResponse(product, available);
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
    @Transactional(readOnly = true)
    public List<ProductAvailableStockResponse> getAvailableQuantities(List<Long> productIds) {
        return stockRepository.findByProductIds(productIds).stream()
                .map(s -> {
                    long qty = s.getStockQuantity() == null ? 0L : s.getStockQuantity();
                    long res = s.getReserved() == null ? 0L : s.getReserved();
                    long available = Math.max(qty - res, 0L);
                    return new ProductAvailableStockResponse(s.getProduct().getId(), available);
                })
                .toList();
    }
}
