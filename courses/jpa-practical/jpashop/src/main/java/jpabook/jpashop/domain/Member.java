package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter  //실무에서는 Setter 를 사용하지 않고 별도의 메서드를 따로 만든다.
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded  //@Embedded, @Embeddable 둘 중 하나만 있어도된다.
    private Address address;

    @OneToMany(mappedBy = "member")  //Order 의 member 를 연관 관계의 주인으로 한다.
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

}
