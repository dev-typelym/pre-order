package com.app.preorder.common.dto;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.common.type.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private ProductStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
