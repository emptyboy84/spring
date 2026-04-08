package com.example.demo; // ⚠️ 주의: 본인의 프로젝트 패키지 이름으로 맞춰주세요! (맨 윗줄 파일 참고)

import java.net.http.HttpHeaders;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/company")
@CrossOrigin(origins = "*") // 나중에 Next.js 화면과 연결하기 위해 통신을 허용하는 설정
public class CompanyController {

   // 🔑 공공데이터포털에서 발급받은 일반 인증키(Decoding)를 아래 큰따옴표 안에 넣으세요!
   private final String SERVICE_KEY = "20496d3131ebf6eca6e9a36ab681096cabac3952f904ff33ab8fbcf2b34bb510";

   @GetMapping("/status")
   public ResponseEntity<?> getCompanyStatus(@RequestParam String bizNumber) {
      // 공공데이터포털 사업자 상태조회 API 주소
      String url = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=" + SERVICE_KEY;

      RestTemplate restTemplate = new RestTemplate();

      // 국세청 API가 요구하는 질문 양식 만들기: { "b_no": ["1234567890"] }
      Map<String, List<String>> body = new HashMap<>();
      // 사용자가 하이픈(-)을 넣어도 알아서 빼고 숫자만 보내도록 처리
      body.put("b_no", Collections.singletonList(bizNumber.replace("-", "")));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

      try {
         // 공공데이터 서버로 요청을 보내고 답변 받아오기
         ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
         return ResponseEntity.ok(response.getBody());
      } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body("서버 통신 중 에러가 발생했습니다: " + e.getMessage());
      }
   }
}