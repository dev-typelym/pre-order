package com.app.preorder.authservice.dto.response;

import com.app.preorder.common.dto.TokenPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {
    private Long id;
    private String loginId;
    private String role;

    public static AuthUserResponse of(TokenPayload payload) {
        return new AuthUserResponse(
                payload.getId(),
                payload.getUsername(),
                payload.getRole().name()
        );
    }
}