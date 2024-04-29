package com.app.preorder.service.member;

import com.app.preorder.entity.member.Member;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class SecurityMember extends User {
    private static final long serialVersionUiD = 1L;

    public SecurityMember(Member member){
        super(member.getUsername(),"{noop}"+ member.getMemberPassword(), AuthorityUtils.createAuthorityList(member.getMemberRole().toString()));
    }
}
