package jpabook.jpashop.api;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    //엔티티를 외부에 노출하는 방식으로 실무에서 권장되지 않는다.
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {

        //아래 findAllByString 만 실행하면 Jackson이 별도의 순환 참조 처리 없이 계속 탐색하며 무한 루프 발생한다.
        //그리고 Order 의 Member 가 지연 로딩으로 인해 프록시를 참조하고 있으므로 Jackson이 이를 처리할 수 없어 에러가 발생한다.
        //이를 해결하기 위해서는 양방향 연관관계에서 어느 한 쪽에 @JasonIgnore 를 붙여 끊어줘야한다.
        //그리고 Hibernate5JakartaModule 을 등록한다. (지연 로딩을 무시하게 된다.)
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        //지연 로딩을 무시하는 상황에서 필요한 값을 가져오는 방법 -> 지연 로딩 강제 초기화
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    //엔티티를 조회해서 DTO 로 변환한다.
    //쿼리를 N번 호출한다는 문제를 가지고 있다.
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(toList());

        return result;
    }

    //엔티티를 조회해서 DTO 로 변환한다.
    //N+1 문제를 해결하기 위해 fetch join 을 사용한다.
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(toList());
        return result;
    }

    //JPA 에서 DTO 를 바로 조회한다.
    //처음부터 엔티티에서 내가 원하는 대상만 가져올 수 있다.
    //성능상 이점은 있지만 (큰 차이가 있지는 않음) 재사용성은 낮다. (다른 api 에서는 사용하기 어려울 수 있다.)
    //api 스펙이 리포지토리까지 들어가게 됐다.
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;  //주문시간
        private OrderStatus orderStatus;
        private Address address;

        //중요하지 않은 곳에서 중요한 곳을 의존하기 때문에 문제되지 않는다.
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }

    }

}
