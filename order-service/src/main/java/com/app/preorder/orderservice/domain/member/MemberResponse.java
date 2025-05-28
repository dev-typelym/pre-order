package com.app.preorder.orderservice.domain.member;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String username;
    private String email;
}
