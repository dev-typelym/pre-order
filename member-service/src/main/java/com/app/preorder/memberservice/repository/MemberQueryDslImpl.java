package com.app.preorder.memberservice.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory query;


}
