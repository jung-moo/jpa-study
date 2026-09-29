package jpabook.jpashop.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

  private final MemberService memberService;

  // RequestBody: json 으로 들어온 데이터를 Member 에 맵핑해준다. (json 외 다른 형식으로도 스프링에서 변경 가능)
  @PostMapping("/api/v1/members")
  public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
    Long id = memberService.join(member);
    return new CreateMemberResponse(id);
  }

  @PostMapping("/api/v2/members")
  public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {

    Member member = new Member();
    member.setName(request.getName());

    Long id = memberService.join(member);
    return new CreateMemberResponse(id);
  }

  @PatchMapping("/api/v2/members/{id}")
  public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, @RequestBody @Valid UpdateMemberRequest request) {
    memberService.update(id, request.getName());  // 변경감지 사용
    Member findMember = memberService.findOne(id);
    return new UpdateMemberResponse(findMember.getId(), findMember.getName());
  }

  @GetMapping("/api/v1/members")
  public List<Member> membersV1() {
    return memberService.findMembers();
  }

  @GetMapping("/api/v2/members")
  public Result membersV2() {

    List<Member> findMembers = memberService.findMembers();
    //엔티티 -> DTO 변환
    List<MemberDto> collect = findMembers.stream()
        .map(m -> new MemberDto(m.getName()))
        .collect(Collectors.toList());

    return new Result(collect);
    //return new Result(collect, collect.size());
  }

  @Data
  @AllArgsConstructor
  static class Result<T> {
    private T data;
    //private int count;  // 이와같이 확장성 증가
  }

  @Data
  @AllArgsConstructor
  static class MemberDto {
    private String name;
  }

  @Data
  static class CreateMemberRequest {
    private String name;
  }

  @Data
  static class CreateMemberResponse {
    private Long id;

    public CreateMemberResponse(Long id) {
      this.id = id;
    }
  }

  @Data
  static class UpdateMemberRequest {
    private String name;
  }

  @Data
  @AllArgsConstructor
  static class UpdateMemberResponse {
    private Long id;
    private String name;
  }

}
