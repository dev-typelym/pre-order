package com.app.preorder.common.dto;


import com.app.preorder.common.type.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayload {
    private Long id;
    private String username;
    private Role role;
    private String deviceId;
}