/*프론트엔드와 백엔드를 연결하는 통로*/
package com.example.demo; // ⚠️ 주의: 본인의 프로젝트 패키지 이름으로 맞춰주세요! (맨 윗줄 파일 참고)

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

/*
 * =====================================================================================
 * 🚀 [전체 데이터 흐름 구조 도식화 (Business Logic Flow)] 🚀
 * =====================================================================================
 * 
 *  1️⃣ [프론트엔드 (React / Next.js 등)] 
 *           │
 *           │ 사용자가 사업자번호 입력 후 '조회' 버튼 클릭
 *           │ 👉 GET /api/company/status?bizNumber=123-45-67890 (서버로 요청 발송)
 *           ▼
 *  2️⃣ [백엔드: CompanyController (@RestController)]
 *           │
 *           │ - 클라이언트의 요청 접수 및 URL 맵핑 (@GetMapping)
 *           │ - 전달받은 사업자번호에서 하이픈(-) 제거 (예: "1234567890")
 *           │ - RestTemplate을 통해 국세청 서버에 보낼 JSON 양식 생성 및 세팅
 *           ▼
 *  3️⃣ [외부 API: 국세청 사업자 상태조회 공공데이터 API]
 *           │
 *           │ - 만들어진 JSON 형식으로 API 호출 (POST 요청 발송)
 *           │ - 국세청 서버가 해당 사업자번호 조회 후 결과(JSON) 반환
 *           ▼
 *  4️⃣ [JSON 데이터 파싱 및 DB 저장 준비 (CompanyController 내부)]
 *           │
 *           │ - 국세청이 준 복잡한 JSON 답변 중 필요한 부분인 "b_stt" (사업자 상태) 찾기
 *           │ - ObjectMapper를 이용해 영업상태 (예: "계속사업자", "휴업자" 등) 문자열 추출
 *           ▼
 *  5️⃣ [데이터베이스 저장: SearchHistoryRepository (@Autowired, MongoDB)]
 *           │
 *           │ - 검색된 사업자번호와 그 상태 결과를 "검색 히스토리"로 DB에 기록
 *           │ - .save(history)를 호출하여 실제 데이터베이스 저장 수행
 *           ▼
 *  6️⃣ [프론트엔드로 응답 반환]
 *           │
 *           │ - DB 저장 완료 후, 국세청에서 받은 전체 원본 JSON 응답값을
 *           │   ResponseEntity.ok(jsonResponse)로 프론트엔드 화면으로 전달
 *           ▼
 *  ✅ [프론트엔드 화면 출력] 👉 "조회하신 사업자는 '계속사업자' 입니다."
 * 
 * =====================================================================================
 */
@RestController
@RequestMapping("/api/company") // 기본경로
@CrossOrigin(origins = "*") // 나중에 Next.js 화면과 연결하기 위해 통신을 허용하는 설정
public class CompanyController {

   // 🔑 공공데이터포털에서 발급받은 일반 인증키(Decoding)를 아래 큰따옴표 안에 넣으세요!
   private final String SERVICE_KEY = "20496d3131ebf6eca6e9a36ab681096cabac3952f904ff33ab8fbcf2b34bb510";// 인증키
   // 🔑 비즈노 API 키 (발급받은 키를 여기에 넣으세요!)
   private final String BIZNO_API_KEY = "G0upKhSPI3R4ByeUI0bWIf5QIvtN";

   // 💡 DB 통신 창구를 불러옵니다. (메소드 바깥인 클래스 레벨 필드로 선언하여 의존성 주입을 받아야 합니다)
   @Autowired
   private SearchHistoryRepository searchHistoryRepository;

   // @RequestParam을 지워도 String 같은 기본 데이터 타입이면 알아서 맵핑 해줍니다.
   @GetMapping("/status") // 상세경로
   public ResponseEntity<?> getCompanyStatus(@RequestParam String bizNumber) {// Query 문자열
      String cleanBizNumber = bizNumber.replace("-", "");// 하이픈 제거

      // 공공데이터포털 사업자 상태조회 API 주소
      String ntsUrl = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=" + SERVICE_KEY;
      // 비즈노 API 주소
      String biznoUrl = "https://www.bizno.net/api/fapi?key=" + BIZNO_API_KEY + "&gb=1&q=" + cleanBizNumber + "&type=json";

      // 국세청 API가 요구하는 질문 양식 만들기: { "b_no": ["1234567890"] }
      Map<String, List<String>> body = new HashMap<>();// 질문할 양식 만들기
      body.put("b_no", Collections.singletonList(cleanBizNumber));// 사업자번호 넣기
      HttpHeaders headers = new HttpHeaders();// 헤더 설정
      headers.setContentType(MediaType.APPLICATION_JSON);// json 형식으로 보내겠다고 선언
      HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);// 헤더와 바디를 합쳐서 요청 객체 생성

      RestTemplate restTemplate = new RestTemplate(); // 스프링이 외부와 통신할 수 있게 해주는 도구
      ObjectMapper objectMapper = new ObjectMapper();

      String realCompanyName = "이름미상"; // 진짜 회사이름을 담을 변수(기본값)

      try {
         // 1. 국세청 서버로 요청을 보내고 사업자 영업상태 받아오기
         ResponseEntity<String> ntsResponse = restTemplate.postForEntity(ntsUrl, request, String.class);
         JsonNode ntsRoot = objectMapper.readTree(ntsResponse.getBody());
         String b_stt = ""; // 영업상태를 담을 변수

         // 2. 비즈노 서버로 요청을 보내고 상호명(회사 이름) 가져오기
         try {
            ResponseEntity<String> biznoRes = restTemplate.getForEntity(biznoUrl, String.class);
            JsonNode biznoRoot = objectMapper.readTree(biznoRes.getBody());
            
            if (biznoRoot.has("items") && biznoRoot.get("items").isArray() && biznoRoot.get("items").size() > 0) {
               JsonNode firstItem = biznoRoot.get("items").get(0);
               if (firstItem.hasNonNull("company")) {
                  realCompanyName = firstItem.get("company").asText();
               } else if (firstItem.hasNonNull("company_name")) {
                  realCompanyName = firstItem.get("company_name").asText();
               }
            }

            // 첫 번째 접속 (bizno.net) 에서 상호명을 못 찾은 경우 두 번째 API 도메인 (bizno.or.kr) 으로 재시도 (Fallback 방어 코드)
            if ("이름미상".equals(realCompanyName)) {
               String fallbackBiznoUrl = "https://api.bizno.or.kr/openapi/v2/company/" + cleanBizNumber + "?apiKey=" + BIZNO_API_KEY;
               ResponseEntity<String> fallbackRes = restTemplate.getForEntity(fallbackBiznoUrl, String.class);
               JsonNode fallbackRoot = objectMapper.readTree(fallbackRes.getBody());
               
               if (fallbackRoot.has("items") && fallbackRoot.get("items").isArray() && fallbackRoot.get("items").size() > 0) {
                  JsonNode fItem = fallbackRoot.get("items").get(0);
                  if (fItem.hasNonNull("company_name")) {
                     realCompanyName = fItem.get("company_name").asText();
                  } else if (fItem.hasNonNull("company")) {
                     realCompanyName = fItem.get("company").asText();
                  }
               }
            }
         } catch (Exception e) {
            System.out.println("비즈노 API 호출 실패 (상호명 조회 실패): " + e.getMessage());
         }

         // 3. 데이터 합치기
         // 몽고디비에 저장하기 위해 받아온 국세청 데이터(JSON)에서 '영업상태(b_stt)' 부분만 살짝 꺼냅니다.
         if (ntsRoot.has("data") && ntsRoot.get("data").isArray() && ntsRoot.get("data").size() > 0) { 
            ObjectNode dataNode = (ObjectNode) ntsRoot.get("data").get(0);

            // 프론트엔드에 보낼 JSON 데이터 안에 비즈노에서 찾은 진짜 상호명을 몰래 끼워 넣습니다.
            dataNode.put("real_company_name", realCompanyName);
            
            // 영업상태 추출
            b_stt = dataNode.get("b_stt").asText(); 
         }

         // 4. DB에 저장! (사업자번호, 영업상태, 상호명)
         if (!b_stt.isEmpty()) {
            SearchHistory history = new SearchHistory(bizNumber, b_stt, realCompanyName);
            searchHistoryRepository.save(history);
         }

         // 5. 프론트엔드로 조작된 전체 데이터 (상호명 포함된 국세청 JSON) 응답 반환
         return ResponseEntity.ok(objectMapper.writeValueAsString(ntsRoot));

      } catch (Exception e) {
         Map<String, String> errorResponse = new HashMap<>();
         errorResponse.put("error", "서버 통신 중 에러가 발생했습니다: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // 에러가 발생했을 때의 상태코드
               .body(errorResponse);
      }
   }
}
