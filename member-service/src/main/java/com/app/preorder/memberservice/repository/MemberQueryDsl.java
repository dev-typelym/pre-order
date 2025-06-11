package com.app.preorder.memberservice.repository;


public interface MemberQueryDsl {

    // 아이디 중복 체크
    public Long overlapByMemberId_QueryDSL(String memberEmail);

    // 이메일 중복 체크
    public Long overlapByMemberEmail_QueryDSL(String memberEmail);

    // 휴대폰 중복 체크
    public Long overlapByMemberPhone_QueryDSL(String memberPhone);


}
