package study.datajpa.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.Collection;
import java.util.List;


public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    //직접 구현하지 않아도 spring-data-jpa 가 구현해준다.
    //간단한 쿼리에 대해 많
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    //오타 등으로 jpql 이 잘못 입력됐을 때 AP 기동 시점에 에러 띄워준다.
    //정적 쿼리는 이 방식 이용하고 동적 쿼리는 querydsl 이용한다.
    @Query("select m from Member m where m.username= :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    //엔티티 조회가 아닌 값 조회
    @Query("select m.username from Member m")
    List<String> findUsernameList();

    //엔티티 조회가 아닌 DTO 로 조회
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) " +
            "from Member m join m.team t")
    List<MemberDto> findMemberDto();

    //컬렉션 파라미터 바인딩
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);

    //다양한 반환타입을 사용할 수 있다.

    //컬렉션
    //값이 없으면 빈 컬렉션을 반환한다. null 반환되지 않는다.
    //List<Member> findByUsername(String name);

    //단건
    //값이 없으면 null 반환한다. -> 값이 없을 수도 있는 경우에는 Optional 을 사용한다.
    //값이 여러개인 경우 예외 발생한다.
    //Member findByUsername(String name);

    //단건 Optional
    //Optional<Member> findByUsername(String name);

    //PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
    //Page<Member> page = memberRepository.findByAge(10, pageRequest);
    //MemberRepositoryTest.java 에서 사용 방법 확인
    //페이지 0 부터 시작한다.
    //반환 타입을 Slice 로 바꾸면 페이징 조회 방식을 바로 바꿀 수 있다.
    //페이지를 유지하면서 엔티티를 DTO 로 변환하기
    //Page<MemberDto> dtoPage = page.map(m -> new MemberDto());
    Page<Member> findByAge(int age, Pageable pageable);

    //쿼리가 복잡해지면 카운트 쿼리를 분리해 성능을 향상 시킬 수 있다.
    //조인을 하는 상황에서 카운트는 굳이 조인을 하지 않아도 되는 경우 쿼리를 분리하면 성능이 향상된다.
    @Query(value = "select m from Member m",
            countQuery = "select count(m.username) from Member m")
    Page<Member> findMemberAllCountBy(Pageable pageable);

    //벌크 연산 이후에는 영속성 컨테이너 초기화를 해줘야한다. (clearAutomatically = true)
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    //페치 조인
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    //내부적으로는 페치 조인을 사용한다.
    //EntityGraph 는 fetch join 이라고 생각하면 된다.
    //복잡한 쿼리는 fetch join 사용하고 간단한 쿼리를 편하게 하고싶을 땐 EntityGraph 사용한다.
    @Override
    @EntityGraph(attributePaths = {"team"})
    List<Member> findAll();

    //JPQL + 엔티티 그래프
    //쿼리를 우선 짜고 페치 조인을 사용하는 것도 가능하다.
    @EntityGraph(attributePaths = {"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    //메서드 이름으로 조회할 때 엔티티 그래프 사용
    @EntityGraph(attributePaths = {"team"})
    List<Member> findByUsername(String username);

    //DB 에 전달하는 힌트가 아닌 JPA 구현체에 전달하는 힌트다.
    //변경 감지 등을 위한 부가적인 기능(스냅샷 생성 등)을 사용하지 않고 조회용으로만 데이터를 가져오도록 힌트를 준다.
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    //List<Member> findByUsername(String name);

}
