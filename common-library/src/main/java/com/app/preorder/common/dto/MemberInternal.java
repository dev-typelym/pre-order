package com.app.preorder.common.dto;

import com.app.preorder.common.type.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberInternal {
    private Long id;
    private String username;
    private MemberStatus status;
}
