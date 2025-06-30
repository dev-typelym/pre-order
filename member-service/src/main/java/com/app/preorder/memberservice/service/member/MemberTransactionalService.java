package com.app.preorder.memberservice.service.member;

import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.request.SignupRequest;
import com.app.preorder.memberservice.factory.MemberFactory;
import com.app.preorder.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberTransactionalService {

    private final MemberRepository memberRepository;
    private final MemberFactory memberFactory;

    @Transactional
    public Long saveMember(SignupRequest request) {
        Member member = memberFactory.createMember(request);
        memberRepository.save(member);
        return member.getId();
    }

    @Transactional
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
    }
}