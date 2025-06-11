package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory query;



    // 아이디 중복 체크
    @Override
    public Long overlapByMemberId_QueryDSL(String username) {
        return query.select(QMember.member.count()).from(QMember.member).where(QMember.member.username.eq(username)).fetchOne();
    }

    // 이메일 중복 체크
    @Override
    public Long overlapByMemberEmail_QueryDSL(String memberEmail) {
        return query.select(QMember.member.count()).from(QMember.member).where(QMember.member.memberEmail.eq(memberEmail)).fetchOne();
    }

    // 휴대폰 중복 체크
    @Override
    public Long overlapByMemberPhone_QueryDSL(String memberPhone) {
        return query.select(QMember.member.count()).from(QMember.member).where(QMember.member.memberPhone.eq(memberPhone)).fetchOne();
    }
}
