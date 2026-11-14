package jpabook.jpashop.repository.order.simplequery;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    //엔티티에서 내가 원하는 대상만 가져올 수 있다.
    //api 스펙이 리포지토리까지 들어왔다.
    //@메모: fetch join 은 엔티티를 조회하면서 연관된 엔티티들도 함께 로딩하는 기능이다. 여기서는 엔티티를 조회하지 않고 DTO 를 조회하므로 일반 join 을 사용하면 된다.
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }

}