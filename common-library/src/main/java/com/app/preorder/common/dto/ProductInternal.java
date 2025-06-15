package com.app.preorder.common.dto;

import com.app.preorder.common.type.CategoryType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInternal {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private CategoryType category;
}
