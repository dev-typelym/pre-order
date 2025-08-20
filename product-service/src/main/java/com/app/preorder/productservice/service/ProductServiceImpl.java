package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.dto.product.*;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductFactory productFactory;
    private final AvailableCacheService availableCache; // ✅ 캐시 서비스 주입

    // 상품 등록
    @Override
    public Long createProduct(ProductCreateRequest request) {
        Product product = productFactory.createFrom(request);
        productRepository.save(product);
        return product.getId();
    }

    // 상품 수정
    @Override
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));
        productFactory.updateFrom(request, product);
    }

    // 상품 삭제
    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));
        productRepository.delete(product);
    }

    // 상품 목록 조회(페이지네이션 + 검색/카테고리 필터, 응답에 가용재고 포함)
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType) {
        Page<Product> products = productRepository.findAllBySearchConditions(PageRequest.of(page, 5), searchRequest, categoryType);

        List<Product> content = products.getContent();
        List<Long> productIds = content.stream().map(Product::getId).toList();

        // ✅ 다건 캐시 조회 → 캐시 미스만 DB (AvailableCacheService 내부에서 처리)
        Map<Long, Long> availableMap = availableCache.getMany(productIds);

        List<ProductResponse> responses = content.stream()
                .map(p -> productFactory.toResponse(p, availableMap.getOrDefault(p.getId(), 0L)))
                .toList();

        return new PageImpl<>(responses, products.getPageable(), products.getTotalElements());
    }

    // 상품 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {
        var product = productRepository.findByIdWithStock(productId)
                .orElseThrow(() -> new ProductNotFoundException("해당 상품을 찾을 수 없습니다."));
        // ✅ 캐시 단건 조회 (DB 미스만 조회)
        long available = availableCache.get(productId);
        return productFactory.toResponse(product, available);
    }

    // 상품 단건 내부 조회(Feign용)
    @Override
    @Transactional(readOnly = true)
    public ProductInternal getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("해당 상품을 찾을 수 없습니다."));
        return productFactory.toProductInternal(product);
    }

    // 상품 다건 내부 조회(Feign용)
    @Override
    @Transactional(readOnly = true)
    public List<ProductInternal> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ProductNotFoundException("일부 상품을 찾을 수 없습니다.");
        }
        return products.stream()
                .map(productFactory::toProductInternal)
                .toList();
    }

    // 가용재고 단건 조회(숫자만 반환) — 폴링/배지용 경량 API
    @Override
    @Transactional(readOnly = true)
    public long getAvailable(Long productId) {
        // ✅ 캐시 단건
        return availableCache.get(productId);
    }

    // 가용재고 다건 조회(카트/목록 재동기화용)
    @Override
    @Transactional(readOnly = true)
    public List<ProductAvailableStockResponse> getAvailableQuantities(List<Long> productIds) {
        // ✅ 캐시 다건
        Map<Long, Long> map = availableCache.getMany(productIds);
        return productIds.stream()
                .map(pid -> new ProductAvailableStockResponse(pid, map.getOrDefault(pid, 0L)))
                .toList();
    }
}
