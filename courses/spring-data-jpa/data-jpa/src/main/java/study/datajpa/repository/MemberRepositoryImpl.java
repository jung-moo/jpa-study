package study.datajpa.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import study.datajpa.entity.Member;

import java.util.List;

@RequiredArgsConstructor
//리포지토리명 + Impl 규칙,관례는 지켜줘야한다. (JavaConfig 에서 수정은 가능하지만 권장하지 않는다.)
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m").getResultList();
    }

}
