package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            //33강 상세 설명
            //item 은 영속성 컨테이너에서 관리하지 않는다.
            //DB 에서 새로 가져온 정보로 만들어낸 영속 엔티티를 반환한다.
            //저장 후 사용하기 위해서는 Item mergeItem = em.merge(item) 과 같이 하여 mergeItem 을 사용해야한다.
            //실무에서는 병합 방식을 권장하지 않는다.
            em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i",Item.class).getResultList();
    }

}