package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
//JPA 의 모든 로직들은 트랜잭션 내에서 실행되어야한다.
//readOnly = true 설정해주면 JPA 가 조회 로직에서는 성능 최적화를 해준다.
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    //Setter injection 의 주요 단점: 런타임 중에 누군가 변경할 수 있다.
    //field injection 의 주요 단점: 테스트 시에 테스트용 리포지토리를 주입해주기 어렵다.
    //생성자 주입 방식을 사용한다.
    private final MemberRepository memberRepository;

    @Transactional  //class 에 붙은 @Transactional 보다 우선권을 갖는다.
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        //동시성 문제가 있을 수 있으므로 최후 방어를 위해 unique 특성을 가진 조회 조건을 권장한다.
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if(!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }

}
