package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="orders")  //ORDER 로는 테이블을 만들 수 없어 테이블명은 ORDERS 로 지정해준다.
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    //생성 메서드로만 객체 생성하도록 한다.
    //@NoArgsConstructor(access = AccessLevel.PROTECTED) 로 대체 가능
    /*
    protected Order() {}
    */

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    //연관 관계의 주인으로 설정한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")  // Order 가 갖고 있는 FK
    private Member member;

    //cascade - orderItem 들을 만들고 order 에 넣은 후 orderItem 들을 각각 persist 하지 않고 order 만 해도 적용된다.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    //Delivery 보다는 Order 에 엑세스할 일이 많기 때문에 Order 를 연관 관계의 주인으로 한다. (강제는 아니다.)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // 주문상태 [ORDER, CANCEL]

    //==연관관계 메서드==//

    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    //==생성 메서드==//
    public static Order createOrder(Member member, Delivery delivery,
                                    OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    //==비즈니스 로직==//
    /** 주문 취소 */
    public void cancel() {
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
        }
        //강조를 위해 this 사용
        //JPA 에게 별다른 요청을 하지 않고 이렇게만 해도 변경이 적용된다.
        //커밋 시점(flush 시점)에 DB update 된다. - dirty checking 이용
        this.setStatus(OrderStatus.CANCEL);
        for(OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }

    //==조회 로직==//
    /** 전체 주문 가격 조회 */
    public int getTotalPrice() {
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }

}
