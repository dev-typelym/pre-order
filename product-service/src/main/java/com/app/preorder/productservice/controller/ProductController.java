package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ProductResponse;
import com.app.preorder.productservice.domain.productDTO.ProductListDTO;
import com.app.preorder.productservice.domain.productDTO.ProductListSearch;
import com.app.preorder.productservice.service.ProductService;
import com.app.preorder.productservice.type.CatergoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product/*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping("/bulk")
    public List<ProductResponse> getProductsByIds(@RequestBody List<Long> productIds) {
        return productService.getProductsByIds(productIds);
    }

    //    상품 목록, 상세
    @GetMapping("productList/{page}")
    @ResponseBody
    public Page<ProductListDTO> getParentsBoard(@PathVariable("page") int page, ProductListSearch productListSearch, CatergoryType catergoryType){
        Page<ProductListDTO> productList = productService.getProductListWithPaging(page -1, productListSearch, catergoryType);
        return productList;
    }

}
