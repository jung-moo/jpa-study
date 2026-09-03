package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Delivery {

    @Id
    @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    //“FK가 없는 반대쪽에서는 연관 객체 존재 여부를 알기 어려워 LAZY가 제대로 적용되지 않을 수 있다” 라는 내용이 있지만 우선은 수업을 따라감
    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;  //[READY, COMP]

}
