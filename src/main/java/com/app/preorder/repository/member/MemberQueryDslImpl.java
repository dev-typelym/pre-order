package com.app.preorder.repository.member;

import com.app.preorder.entity.member.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.app.preorder.entity.member.QMember.member;

@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory query;

    // 멤버 아이디로 멤버 찾기
    @Override
    public Member findMemberById(Long memberId){
        return query.select(member).from(member).where(member.id.eq(memberId)).fetchOne();
    }

    // 개인정보 변경
    public void changeMemberInfo_QueryDSL(String name, String email, String phone, String address, String addressDetail, String addressSubDetail, String postCode, Long memberId){
        long updatedRows = query.update(member)
                .set(member.name, name)
                .set(member.memberEmail, email)
                .set(member.memberPhone, phone)
                .set(member.memberAddress.address, address)
                .set(member.memberAddress.addressDetail, addressDetail)
                .set(member.memberAddress.addressSubDetail, addressSubDetail)
                .set(member.memberAddress.postcode, postCode)
                .where(member.id.eq(memberId))
                .execute();

        if (updatedRows != 1) {
            throw new IllegalStateException("Failed to update password for member ID: " + memberId);
        }
    }

    // 회원 비밀번호 변경
    @Override
    public void changePassword_QueryDSL(String password, Long memberId){
        long updatedRows = query.update(member)
                .set(member.memberPassword, password)
                .where(member.id.eq(memberId))
                .execute();

        if (updatedRows != 1) {
            throw new IllegalStateException("Failed to update password for member ID: " + memberId);
        }
    }

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
