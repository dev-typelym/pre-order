package com.app.preorder.authservice.security;

import com.app.preorder.entity.member.Member;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class MemberPrincipal extends User {
    private static final long serialVersionUiD = 1L;

    public MemberPrincipal(Member member){
        super(member.getUsername(),"{noop}"+ member.getMemberPassword(), AuthorityUtils.createAuthorityList(member.getMemberRole().toString()));
    }
}
