package com.app.preorder.memberservice.repository;


import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.domain.entity.QMember;
import com.app.preorder.memberservice.dto.UpdateMemberInfo;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory query;

    // 개인정보 변경
    public void updateMemberById(UpdateMemberInfo updateMemberInfo, Long memberId){
        long updatedRows = query.update(QMember.member)
                .set(QMember.member.name, updateMemberInfo.getName())
                .set(QMember.member.memberEmail, updateMemberInfo.getEmail())
                .set(QMember.member.memberPhone, updateMemberInfo.getPhone())
                .set(QMember.member.memberAddress.address, updateMemberInfo.getAddress())
                .set(QMember.member.memberAddress.addressDetail, updateMemberInfo.getAddressDetail())
                .set(QMember.member.memberAddress.addressSubDetail, updateMemberInfo.getAddressSubDetail())
                .set(QMember.member.memberAddress.postcode, updateMemberInfo.getPostCode())
                .where(QMember.member.id.eq(memberId))
                .execute();

        if (updatedRows != 1) {
            throw new IllegalStateException("Failed to update password for member ID: " + memberId);
        }
    }

    // 회원 비밀번호 변경
    @Override
    public void changePassword_QueryDSL(String password, Long memberId){
        long updatedRows = query.update(QMember.member)
                .set(QMember.member.memberPassword, password)
                .where(QMember.member.id.eq(memberId))
                .execute();

        if (updatedRows != 1) {
            throw new IllegalStateException("Failed to update password for member ID: " + memberId);
        }
    }

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
