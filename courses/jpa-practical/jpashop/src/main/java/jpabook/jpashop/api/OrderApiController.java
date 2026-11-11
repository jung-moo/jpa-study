package jpabook.jpashop.api;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * **중요**
 * 현재 코드는 OSIV 를 켜놓은 상태를 가정하며 끄게 되면 커멘드와 쿼리를 분리하는 구조로 변경해야한다.
 * 실시간 트래픽이 많은 API 는 OSIV 를 끄고, ADMIN 처럼 커넥션을 많이 사용하지 않는 곳에서는 OSIV 를 켜는 것을 권장한다.
 *
 * V1. 엔티티 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다.
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 *
 * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 *
 * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능)
 *
 * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 *
 * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 *
 * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
 * - 페이징 불가능...
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    //첫번째 문제인 양방향 관계 문제가 발행하여 @JsonIgnore 로 해결했다.
    //두번째 문제인 지연 로딩 프록시를 직렬화하지 못하는 문제가 발생하여 Hibernate5Module 모듈 등록과 강제 초기화로 해결했다.
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for (Order order : all) {
            order.getMember().getName();  //Lazy 강제 초기화
            order.getDelivery().getAddress();  //Lazy 강제 초기화
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());  //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 X)
     * - 트랜잭션 안에서 지연 로딩 필요
     */
    //주문 하나에 회원 가져오기, 주소 가져오기, 주문상품들 가져오기, 주문상품1 가져오기, 주문상품2 가져오기 쿼리 실행
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());

        return result;
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 O) - 페이징 불가능
     * - 페이징 시에는 N 부분을 포기해야된다.
     */
    //jpql 에서 JPA distinct 를 이용해 조인 뻥튀기는 제거해준다.
    //JPA 에서 처리하는 것이기 때문에 DB 로부터의 데이터 전송량은 중복이 많다. (불필요 데이터로 인한 성능 저하)
    //페이징을 하려하면 '모든' 데이터를 DB 에서 읽어오고 메모리에서 페이징한다. (위험)
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());

        return result;
    }

    /**
     * V3.1 엔티티를 조회해서 DTO로 변환 페이징 고려 - 페이징 가능
     * - ToOne 관계만 우선 모두 페치 조인으로 최적화 (데이터가 증가하지 않는다.)
     * - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
     */
    //엔티티를 DTO 로 변환하여 반환 방식 1순위
    //컬렉션은 지연 로딩으로 가져온다. (fetch join 을 사용하지 않는다.)
    //V3 에 비해 쿼리는 많이 실행되지만 중복 데이터는 발생하지 않아 데이터 전송량은 적다.
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "limit", defaultValue = "100") int limit) {

        //페이징은 XtoOne 을 대상으로 진행되기 때문에 중복 데이터는 발생하지 않는다.
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());

        return result;
    }

    /**
     * V4. 컬렉션까지 포함해 JPA에서 DTO로 바로 조회한다.
     * - 페이징 가능
     */
    //N+1 문제 발생한다.
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
     * - 페이징 가능
     */
    //JPA 에서 DTO 로 바로 조회 방식 1순위
    //N+1 문제에 대해 최적화한다.
    //N+1 은 아니지만 1+1
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
     * - 페이징 불가능...
     */
    //V5 에서의 1+1 쿼리를 1 쿼리로 실행한다.
    //AP 에 중복 데이터를 더 많이 넘겨준다.
    //AP 에서 추가 작업이 크다.
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        //OrderFlatDto 가 아닌 OrderQueryDto 로 반환하기 위한 작업이다.
        return flats.stream()
            .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
            )).entrySet().stream()
            .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
            .collect(toList());
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        //private List<OrderItem> orderItems;  //OrderItem 엔티티 하나하나가 외부에 노출된다. 엔티티에 대한 의존을 완전히 끊어야 한다.
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();

            //orderItems = order.getOrderItems();  //이렇게만 하면 null 나온다.
            //아래와 하면 orderItems 가 정상적으로 조회된다. 하지만 OrderItem 엔티티 하나하나가 외부에 노출된다. 엔티티에 대한 의존을 완전히 끊어야 한다.
            //order.getOrderItems().stream().forEach(o -> o.getItem().getName());

            orderItems = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemDto(orderItem))
                .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }

    }

}
