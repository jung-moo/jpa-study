package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    //@PersistenceContext  //스프링 데이터 JPA 사용하여 생성자 주입 방식으로 변경한다.
    private final EntityManager em;

    //EntityManagerFactory 를 주입 받는 것도 가능하다.
    //@PersistenceUnits
    //private EntityManagerFactory emf;

    public void save(Member member) {
        //key value 형태로 영속성 컨테이너에 들어간다.
        //DB 의 PK 와 매핑한 컬럼이 key 가 된다.
        em.persist(member);
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        //JPQL - 테이블이 아닌 객체를 대상으로 쿼리를 실행한다.
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }

}
