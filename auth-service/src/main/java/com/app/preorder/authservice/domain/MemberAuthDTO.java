package com.app.preorder.authservice.domain;

@Getter
@NoArgsConstructor
public class MemberAuthDTO {
    private Long id;
    private String username;
    private String password;
    private String role;
}