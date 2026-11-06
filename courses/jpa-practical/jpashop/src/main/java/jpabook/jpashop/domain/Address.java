package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable // @Embedded, @Embeddable 둘 중 하나만 있어도된다.
@Getter  // 변경 불가능하도록 Setter 를 설정하지 않는다.
public class Address {

    private String city;
    private String street;
    private String zipCode;

    // JPA 스펙 상 만드는 기본 생성자
    // 변경 불간능해야 하므로 protected 가 비교적 안전하다.
    protected Address() {
    }

    public Address(String city, String street, String zipCode) {
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
    }
}
