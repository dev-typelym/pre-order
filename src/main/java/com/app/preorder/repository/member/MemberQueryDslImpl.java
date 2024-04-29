package com.app.preorder.repository.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.app.preorder.entity.member.QMember.member;

@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory query;

    // 아이디 중복 체크
    @Override
    public Long overlapByMemberId_QueryDSL(String username) {
        return query.select(member.count()).from(member).where(member.username.eq(username)).fetchOne();
    }

    // 이메일 중복 체크
    @Override
    public Long overlapByMemberEmail_QueryDSL(String memberEmail) {
        return query.select(member.count()).from(member).where(member.memberEmail.eq(memberEmail)).fetchOne();
    }

    // 휴대폰 중복 체크
    @Override
    public Long overlapByMemberPhone_QueryDSL(String memberPhone) {
        return query.select(member.count()).from(member).where(member.memberPhone.eq(memberPhone)).fetchOne();
    }
}
