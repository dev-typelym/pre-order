package com.app.preorder.repository.member;

import com.app.preorder.entity.member.Member;

public interface MemberQueryDsl {

    // 멤버 아이디로 멤버 찾기
    public Member findMemberById(Long memberId);

    // 아이디 중복 체크
    public Long overlapByMemberId_QueryDSL(String memberEmail);

    // 이메일 중복 체크
    public Long overlapByMemberEmail_QueryDSL(String memberEmail);

    // 휴대폰 중복 체크
    public Long overlapByMemberPhone_QueryDSL(String memberPhone);


}
