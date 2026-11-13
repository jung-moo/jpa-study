package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {

  //회원명, 팀명, 나이(ageGoe, ageLoe)
  //지금은 없지만 기본 조건 넣어주는게 좋다. (Service 혹은 Repository 에서 넣어줄 수도 있다. 혹은 페이징 쿼리를 같이 넣는다.)
  private String username;
  private String teamName;
  private Integer ageGoe;
  private Integer ageLoe;

}