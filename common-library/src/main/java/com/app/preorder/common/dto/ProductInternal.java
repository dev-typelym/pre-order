package com.app.preorder.common.dto;

import com.app.preorder.common.type.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInternal {
    private Long id;
    private String name;
    private int price;
}