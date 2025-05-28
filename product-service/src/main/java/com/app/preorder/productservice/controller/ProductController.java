package com.app.preorder.productservice.controller;

import com.app.preorder.domain.productDTO.ProductListDTO;
import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.service.product.ProductService;
import com.app.preorder.type.CatergoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product/*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    //    상품 목록, 상세
    @GetMapping("productList/{page}")
    @ResponseBody
    public Page<ProductListDTO> getParentsBoard(@PathVariable("page") int page, ProductListSearch productListSearch, CatergoryType catergoryType){
        Page<ProductListDTO> productList = productService.getProductListWithPaging(page -1, productListSearch, catergoryType);
        return productList;
    }

}
