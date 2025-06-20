package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductResponse;
import com.app.preorder.productservice.dto.productDTO.ProductSearchRequest;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Transactional
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductFactory productFactory;

    //  상품 목록
    @Override
    public Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType) {
        Page<Product> products = productRepository.findAllBySearchConditions(PageRequest.of(page, 5), searchRequest, categoryType);

        List<ProductResponse> responses = products.getContent().stream()
                .map(productFactory::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, products.getPageable(), products.getTotalElements());
    }

    //  상품 상세
    @Override
    public ProductResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithStocks(productId)
                .orElseThrow(() -> new EntityNotFoundException("해당 상품을 찾을 수 없습니다."));
        return productFactory.toResponse(product);
    }

    //  상품 다건 조회
    @Override
    public List<ProductInternal> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        return products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }
}
