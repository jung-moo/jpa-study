package study.querydsl;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @PersistenceContext
  EntityManager em;

  JPAQueryFactory queryFactory;

  @BeforeEach
  public void before() {

    queryFactory = new JPAQueryFactory(em);

    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

  }

  @Test
  public void startJPQL() {

    String qlString = "select m from Member m where m.username = :username";
    Member findMember = em.createQuery(qlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();
    assertThat(findMember.getUsername()).isEqualTo("member1");

  }

  //qeurydsl 이 jpql 을 만들어준다.
  /*
  @Test
  public void startQuerydsl() {

    QMember m = new QMember("m");

    Member findMember = queryFactory
        .select(m)
        .from(m)
        .where(m.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");

  }
   */

  //위 방식보다는 이렇게 사용을 권장한다.
  @Test
  public void startQuerydsl() {

    //같은 테이블을 조인해야되는 경우 alias 를 변경해준다.
    //이 경우 외에는 사용할 일이 많이 없다.
    //QMember m = new QMember("m1");

    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");

  }

  @Test
  public void search() {
    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void searchAndParam() {
    List<Member> result1 = queryFactory
        .selectFrom(member)
        .where(
            member.username.eq("member1"),
            member.age.eq(10)
        )
        .fetch();
    assertThat(result1.size()).isEqualTo(1);
  }

  @Test
  public void resultFetch() {

    //List
    List<Member> fetch = queryFactory
        .selectFrom(member)
        .fetch();

    //단 건
    Member findMember1 = queryFactory
        .selectFrom(member)
        .fetchOne();

    //처음 한 건 조회
    Member findMember2 = queryFactory
        .selectFrom(member)
        .fetchFirst();

    //페이징에서 사용
    QueryResults<Member> results = queryFactory
        .selectFrom(member)
        .fetchResults();

    results.getTotal();
    List<Member> content = results.getResults();

    //count 쿼리로 변경
    long count = queryFactory
        .selectFrom(member)
        .fetchCount();

  }

  /**
   * 회원 정렬 순서
   * 1. 회원 나이 내림차순(desc)
   * 2. 회원 이름 올림차순(asc)
   * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
   */
  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();
    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);
    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  public void paging1() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)  //0부터 시작(zero index)
        .limit(2)
        .fetch();
    assertThat(result.size()).isEqualTo(2);
  }

  //count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면, count 전용 쿼리를 별도 작성해야한다.
  @Test
  public void paging2() {
    QueryResults<Member> queryResults = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetchResults();
    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }

  //실무에서는 Tuple 이 아닌 DTO 로 받는 방식을 많이 사용한다.
  @Test
  public void aggregation() throws Exception {
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min())
        .from(member)
        .fetch();
    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  @Test
  public void group() throws Exception {
    List<Tuple> result = queryFactory
        .select(
            team.name,
            member.age.avg()
        )
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();
    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);
    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  @Test
  public void join() throws Exception {

    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();
    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  /**
   * 세타 조인(연관관계가 없는 필드로 조인)
   * 회원의 이름이 팀 이름과 같은 회원 조회
   */
  //연관관계가 없어도 조인이 된다.
  @Test
  public void theta_join() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();
    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  /**
   * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
   * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
   * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
   */
  @Test
  public void join_on_filtering() throws Exception {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)  //Member 과 Team 의 team_id 를 조인한다.
        .on(team.name.eq("teamA"))
        .fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  /**
   * 2. 연관관계 없는 엔티티 외부 조인
   * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
   * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
   * SQL: SELECT m.*, t.* FROM  Member m LEFT JOIN Team t ON m.username = t.name
   */
  @Test
  public void join_on_no_relation() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team)  //바로 team 을 넣는다. id 로 조인하지 않는다.
        .on(member.username.eq(team.name))
        .fetch();
    for (Tuple tuple : result) {
      System.out.println("t=" + tuple);
    }
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  public void fetchJoinNo() throws Exception {
    em.flush();
    em.clear();
    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();
    boolean loaded =
        emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());  //로드(초기화)된 엔티티인지 아닌지 확인한다.
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  public void fetchJoinUse() throws Exception {
    em.flush();
    em.clear();
    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();
    boolean loaded =
        emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
  }

  /**
   * 나이가 가장 많은 회원 조회
   */
  @Test
  public void subQuery() throws Exception {

    //alias 가 중복되면 안되는 경우이다.
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            JPAExpressions  //static import 할 수 있다.
                .select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();
    assertThat(result).extracting("age")
        .containsExactly(40);
  }

  /**
   * 나이가 평균 나이 이상인 회원
   */
  @Test
  public void subQueryGoe() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();
    assertThat(result).extracting("age")
        .containsExactly(30,40);
  }

  /**
   * 서브쿼리 여러 건 처리, in 사용
   */
  @Test
  public void subQueryIn() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            JPAExpressions
                .select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();
    assertThat(result).extracting("age")
        .containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> fetch = queryFactory
        .select(member.username,
            JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub)
        ).from(member)
        .fetch();
    for (Tuple tuple : fetch) {
      System.out.println("username = " + tuple.get(member.username));
      System.out.println("age = " +
          tuple.get(JPAExpressions.select(memberSub.age.avg())
              .from(memberSub)));
    }

  }

  @Test
  public void basicCase() {

    List<String> result = queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("스무살")
            .otherwise("기타"))
        .from(member)
        .fetch();

  }

  @Test
  public void complexCase() {

    List<String> result = queryFactory
        .select(new CaseBuilder()
            .when(member.age.between(0, 20)).then("0~20살")
            .when(member.age.between(21, 30)).then("21~30살")
            .otherwise("기타"))
        .from(member)
        .fetch();

  }

  @Test
  public void caseWithOrderby() {

    NumberExpression<Integer> rankPath = new CaseBuilder()
        .when(member.age.between(0, 20)).then(2)
        .when(member.age.between(21, 30)).then(1)
        .otherwise(3);
    List<Tuple> result = queryFactory
        .select(member.username, member.age, rankPath)
        .from(member)
        .orderBy(rankPath.desc())
        .fetch();
    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      Integer rank = tuple.get(rankPath);
      System.out.println("username = " + username + " age = " + age + " rank = " +
          rank);
    }

  }

  @Test
  public void constant() {

    Tuple result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetchFirst();

    System.out.print("result = " + result);

  }

  @Test
  public void concat() {

    String result = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

  }

  @Test
  public void simpleProjection() {

    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

  }

  //com.querydsl.core 이다. 외부 계층으로 빠져나가지 않도록 해야된다. 밖으로 나갈 때는 DTO 로 변환한다.
  @Test
  public void tupleProjection() {
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();
    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      System.out.println("username=" + username);
      System.out.println("age=" + age);
    }
  }

  @Test
  public void findDtoByJPQL() {

    List<MemberDto> result = em.createQuery(
            "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m"
            , MemberDto.class)
        .getResultList();

  }

  @Test
  public void findDtoBySertter() {

    //Getter, Setter 가 있어야한다.
    List<MemberDto> result = queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.print("memberDto = " + memberDto);
    }

  }

  @Test
  public void findDtoByField() {

    //Getter, Setter 가 없어도 된다.
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.print("memberDto = " + memberDto);
    }

  }

  @Test
  public void findDtoByConstructor() {

    List<MemberDto> result = queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.print("memberDto = " + memberDto);
    }

  }

  @Test
  public void findUserDto() {

    QMember memberSub = new QMember("memberSub");
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username.as("name"),
            ExpressionUtils.as(
                    JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
            )
        ).from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.print("memberDto = " + memberDto);
    }

  }

  @Test
  public void findDtoByQueryProjection() {

    List<MemberDto> result = queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.print("memberDto = " + memberDto);
    }

  }

  @Test
  public void 동적쿼리_BooleanBuilder() throws Exception {
    String usernameParam = "member1";
    Integer ageParam = 10;
    List<Member> result = searchMember1(usernameParam, ageParam);
    Assertions.assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameCond, Integer ageCond) {
    //null 이 넘어오지 않는다는걸 가정했을 때 (방어코드가 있을 때) 아래와 같이 초기값을 넣어줄 수도 있다.
    //BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
    BooleanBuilder builder = new BooleanBuilder();
    if (usernameCond != null) {
      builder.and(member.username.eq(usernameCond));
    }
    if (ageCond != null) {
      builder.and(member.age.eq(ageCond));
    }
    return queryFactory
        .selectFrom(member)
        .where(builder)
        .fetch();
  }

  //Builder 보다 Where 절 파라미터를 더 많이 사용한다.
  @Test
  public void 동적쿼리_WhereParam() throws Exception {
    String usernameParam = "member1";
    Integer ageParam = 10;
    List<Member> result = searchMember2(usernameParam, ageParam);
    Assertions.assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember2(String usernameCond, Integer ageCond) {
    return queryFactory
        .selectFrom(member)
        .where(usernameEq(usernameCond), ageEq(ageCond))  //매개변수가 null 이면 무시하므로 동적쿼리가 된다.
        .fetch();
  }

  private BooleanExpression usernameEq(String usernameCond) {
    return usernameCond != null ? member.username.eq(usernameCond) : null;
  }

  private BooleanExpression ageEq(Integer ageCond) {
    return ageCond != null ? member.age.eq(ageCond) : null;
  }

  //'광고 상태가 진행 중이고 날짜가 범위 안에 있다' 를 isServicable 로 만들어 한 번에 처리할 수 있으며 재사용도 가능하다.
  private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
  }

  @Test
  @Commit
  public void bulkUpdate() {
    long count = queryFactory
        .update(member)
        .set(member.username, "비회원")
        .where(member.age.lt(28))
        .execute();
    em.flush();
    em.clear();
  }

  @Test
  public void bulkAdd() {
    long count = queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .execute();
    em.flush();
    em.clear();
  }

  @Test
  public void bulkDelete() {
    long count = queryFactory
        .delete(member)
        .where(member.age.gt(18))
        .execute();
    em.flush();
    em.clear();
  }

  @Test
  public void sqlFunction() {

    String result = queryFactory
        .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
            member.username, "member", "M"))
        .from(member)
        .fetchFirst();

    System.out.println("result = " + result);

  }

  @Test
  public void sqlFunction2() {

    String result = queryFactory
        .select(member.username)
        .from(member)
        //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
        .where(member.username.eq(member.username.lower()))
        .fetchFirst();

    System.out.println("result = " + result);

  }

}