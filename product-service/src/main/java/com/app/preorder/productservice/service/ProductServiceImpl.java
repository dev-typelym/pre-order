package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductListDTO;
import com.app.preorder.productservice.dto.productDTO.ProductListSearch;
import com.app.preorder.productservice.domain.entity.Product;
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

    //  상품 목록
    @Override
    public Page<ProductListDTO> getProductListWithPaging(int page, ProductListSearch productListSearch, CategoryType categoryType) {
        Page<Product> products = productRepository.findAllProduct_queryDSL(PageRequest.of(page, 5), productListSearch, categoryType);
        List<ProductListDTO> productListDTOS = products.getContent().stream()
                .map(this::toProductListDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(productListDTOS, products.getPageable(), products.getTotalElements());
    }

    //  상품 상세
    @Override
    public List<ProductListDTO> getProductDetail() {
        List<Product> productDetail = productRepository.findAllProductDetail_queryDSL();
        List<ProductListDTO> productDetailDTOS = productDetail.stream()
                .map(this::toProductListDTO)
                .collect(Collectors.toList());
        return productDetailDTOS;
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
