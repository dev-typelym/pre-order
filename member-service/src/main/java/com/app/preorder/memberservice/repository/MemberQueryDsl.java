package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.Member;

public interface MemberQueryDsl {


    // 개인정보 변경
    public void changeMemberInfo_QueryDSL(String name, String email, String phone, String address, String addressDetail, String addressSubDetail, String postCode, Long memberId);

    // 비밀번호 변경
    public void changePassword_QueryDSL(String password, Long memberId);

    // 아이디 중복 체크
    public Long overlapByMemberId_QueryDSL(String memberEmail);

    // 이메일 중복 체크
    public Long overlapByMemberEmail_QueryDSL(String memberEmail);

    // 휴대폰 중복 체크
    public Long overlapByMemberPhone_QueryDSL(String memberPhone);


}
