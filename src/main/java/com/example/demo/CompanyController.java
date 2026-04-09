package com.example.demo; // ⚠️ 주의: 본인의 프로젝트 패키지 이름으로 맞춰주세요! (맨 윗줄 파일 참고)

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/*어노테이션(@)을 한마디로 표현하자면 "스프링(Spring) 프레임워크에게 내리는 마법의 지시사항(포스트잇)" 입니다.
코드 바로 위에 딱지(포스트잇)를 붙여서 직관적으로 명령을 내리자고 만든 것이 어노테이션입니다.
1. 엄청난 양의 코드를 대신 짜줍니다 (자동화 마법)
사실 자바(Java) 언어 자체로 "웹 서버를 띄우고, 인터넷 접속을 받아서, 글자를 화면에 띄워라" 라고 명령하려면 정말 복잡한 통신 코드를 수십 줄 짜야 합니다. 하지만 클래스 위에 @RestController 딱 하나만 붙여주면?

Spring의 반응: "아! 이 클래스는 일반 자바 코드가 아니라 웹 브라우저의 요청을 받아주는 컨트롤러구나! 내가 뒤에서 복잡한 통신 설정, 데이터 변환(JSON) 다 해줄게!"

2. 길 안내 (라우팅) 역할
메소드 위에 **@GetMapping("/status")**를 붙여주면?

Spring의 반응: "만약 사용자가 브라우저에서 localhost:8080/api/company/status 로 들어오면, 아묻따(아무것도 묻고 따지지 않고) 밑에 있는 getCompanyStatus 함수를 실행시켜줘야겠다!"

3. 귀찮은 보안 설정도 한 줄로 끝!
클래스 위에 **@CrossOrigin(origins = "*")**을 붙여주셨죠?

Spring의 반응: "원래 웹 브라우저(Next.js)가 다른 포트(8080)로 몰래 데이터 가져가는 거 해킹인 줄 알고 무조건 차단해야 하는데(CORS 에러), 이 포스트잇이 붙어있으니 프론트엔드 코드의 접근을 특별히 허가해 줘야겠다!"

💡정리하자면: 어노테이션은 자바 언어만으로는 귀찮고 복잡한 설정들을 단어 하나(@~~)만 적어서 스프링이 알아서 다 세팅하게 만드는 엄청나게 편리한 치트키라고 보시면 됩니다!*/
@RestController
@RequestMapping("/api/company") // 기본경로
@CrossOrigin(origins = "*") // 나중에 Next.js 화면과 연결하기 위해 통신을 허용하는 설정
public class CompanyController {

   // 🔑 공공데이터포털에서 발급받은 일반 인증키(Decoding)를 아래 큰따옴표 안에 넣으세요!
   private final String SERVICE_KEY = "20496d3131ebf6eca6e9a36ab681096cabac3952f904ff33ab8fbcf2b34bb510";// 인증키

   // @RequestParam을 지워도 String 같은 기본 데이터 타입이면 알아서 맵핑 해줍니다.
   @GetMapping("/status") // 상세경로
   public ResponseEntity<?> getCompanyStatus(@RequestParam String bizNumber) {// Query 문자열
      // 공공데이터포털 사업자 상태조회 API 주소
      String url = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=" + SERVICE_KEY;

      RestTemplate restTemplate = new RestTemplate();

      // 국세청 API가 요구하는 질문 양식 만들기: { "b_no": ["1234567890"] }
      Map<String, List<String>> body = new HashMap<>();
      // 사용자가 하이픈(-)을 넣어도 알아서 빼고 숫자만 보내도록 처리
      body.put("b_no", Collections.singletonList(bizNumber.replace("-", "")));// 하이픈 제거

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

      try {
         // 공공데이터 서버로 요청을 보내고 답변 받아오기
         ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
         return ResponseEntity.ok(response.getBody());
      } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)// 에러가 발생했을 때의 상태코드
               .body("서버 통신 중 에러가 발생했습니다: " + e.getMessage());
      }
   }
}