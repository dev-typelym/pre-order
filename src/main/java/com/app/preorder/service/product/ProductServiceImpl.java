package com.app.preorder.service.product;

import com.app.preorder.domain.product.ProductListDTO;
import com.app.preorder.domain.product.ProductListSearch;
import com.app.preorder.entity.product.Product;
import com.app.preorder.repository.product.ProductRepository;
import com.app.preorder.type.CatergoryType;
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

    //    상품 목록
    @Override
    public Page<ProductListDTO> getProductListWithPaging(int page, ProductListSearch productListSearch, CatergoryType catergoryType) {
        Page<Product> products = productRepository.findAllProduct_queryDSL(PageRequest.of(page, 5), productListSearch, catergoryType);
        List<ProductListDTO> productListDTOS = products.getContent().stream()
                .map(this::toProductListDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(productListDTOS, products.getPageable(), products.getTotalElements());
    }

    //   상품 상세
    @Override
    public List<ProductListDTO> getProductDetail() {
        List<Product> productDetail = productRepository.findAllProductDetail_queryDSL();
        List<ProductListDTO> productDetailDTOS = productDetail.stream()
                .map(this::toProductListDTO)
                .collect(Collectors.toList());
        return productDetailDTOS;
    }
}
