package jpabook.jpashop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
//테스트에서는 자동으로 롤백해준다.
//DB insert 등의 쿼리를 보고 싶으면 flush 를 따로 해주거나 메서스에 @Rollback(false) 설정을 해준다.
@Transactional
public class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    //@Autowired
    //EntityManager em;  //직접 flush 를 위해 주입받는다.

    @Test
    //@Rollback(false)
    public void 회원가입() throws Exception {

        Member member = new Member();
        member.setName("kim");

        Long saveId = memberService.join(member);

        //em.flush();

        assertEquals(member, memberRepository.findOne(saveId));
    }

    @Test
    public void 중복_회원_예외() throws Exception {

        Member member1 = new Member();
        member1.setName("kim");
        Member member2 = new Member();
        member2.setName("kim");

        memberService.join(member1);

        //IllegalStateException 예외가 발생하지 않으면 테스트 실패
        assertThrows(IllegalStateException.class, () ->
                memberService.join(member2));

    }

}