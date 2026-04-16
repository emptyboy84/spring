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

   // application.properties 에서 외부 환경변수로 관리되는 API 키 3종을 주입받습니다. (@Value 사용)
   @org.springframework.beans.factory.annotation.Value("${api.key.public-data}")
   private String SERVICE_KEY;

   @org.springframework.beans.factory.annotation.Value("${api.key.bizno}")
   private String BIZNO_API_KEY;

   @org.springframework.beans.factory.annotation.Value("${api.key.dart}")
   private String DART_API_KEY;

   // 🚀 회사명 -> DART 고유번호 매핑용 메모리 장부
   private java.util.Map<String, String> companyNameToCorpCodeMap = new java.util.concurrent.ConcurrentHashMap<>();

   // 🏭 고용산재보험 CSV: 사업자등록번호 -> [상시근로자수, 주소, 업종명, 사업장명]
   private java.util.Map<String, String[]> insuranceDataMap = new java.util.concurrent.ConcurrentHashMap<>();

   // 💡 DB 통신 창구를 불러옵니다. (메소드 바깥인 클래스 레벨 필드로 선언하여 의존성 주입을 받아야 합니다)
   @Autowired
   private SearchHistoryRepository searchHistoryRepository;

   // 🚀 서버가 시작될 때 딱 한 번! DART의 모든 기업 고유번호(약 10만건)를 ZIP으로 다운받아 메모리에 적재해둡니다.
   @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
   public void initDartCorpCodes() {
      System.out.println("DART 기업 고유번호(corpCode) 다운로드 및 메모리 적재 시작...");
      try {
         String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + DART_API_KEY;
         java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(url).toURL()
               .openConnection();
         conn.setRequestMethod("GET");
         try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(conn.getInputStream())) {
            if (zis.getNextEntry() != null) {
               javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                     .newDocumentBuilder();
               org.w3c.dom.Document doc = db.parse(zis);
               org.w3c.dom.NodeList list = doc.getElementsByTagName("list");
               for (int i = 0; i < list.getLength(); i++) {
                  org.w3c.dom.Element el = (org.w3c.dom.Element) list.item(i);
                  String corpCode = el.getElementsByTagName("corp_code").item(0).getTextContent();
                  org.w3c.dom.Node nameNode = el.getElementsByTagName("corp_name").item(0);
                  if (nameNode != null) {
                     String cName = nameNode.getTextContent();
                     String cleanName = cName.replace("(주)", "").replace("주식회사", "").replace(" ", "").trim();
                     if (!cleanName.isEmpty())
                        companyNameToCorpCodeMap.put(cleanName, corpCode);
                  }
               }
            }
         }
         System.out.println("✅ DART 서버에서 고유번호 로딩 완료! (총 " + companyNameToCorpCodeMap.size() + "건 확보 마침)");
      } catch (Exception e) {
         System.out.println("❌ DART 고유번호 다운로드 실패: " + e.getMessage());
      }
   }

   // 🏭 서버 시작 시 고용산재보험 CSV(200만 사업장)를 메모리에 적재합니다.
   @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
   public void initInsuranceData() {
      System.out.println("고용산재보험 CSV 데이터 로딩 시작...");
      String csvDir = System.getProperty("user.dir") + "/src/main/resources/insurance";
      java.io.File dir = new java.io.File(csvDir);
      if (!dir.exists() || !dir.isDirectory()) {
         System.out.println("⚠️ insurance 디렉토리 없음: " + csvDir);
         return;
      }
      int loadedCount = 0;
      for (java.io.File csvFile : dir.listFiles((d, name) -> name.endsWith(".csv"))) {
         try (java.io.BufferedReader br = new java.io.BufferedReader(
               new java.io.InputStreamReader(new java.io.FileInputStream(csvFile), "CP949"))) {
            String line = br.readLine(); // 헤더 스킵
            while ((line = br.readLine()) != null) {
               try {
                  // CSV 파싱: 연번,보험구분,사업장명,우편번호,주소,업종코드,업종명,산재성립,...,산재근로자수,고용근로자수,...,사업자등록번호,관리번호
                  String[] cols = line.split(",", -1);
                  if (cols.length >= 14) {
                     String bizNo = cols[13].trim(); // 사업자등록번호
                     if (bizNo.isEmpty()) continue;
                     String empCount = cols[10].trim(); // 고용보험 상시근로자수 (산재보다 정확)
                     if (empCount.isEmpty()) empCount = cols[9].trim(); // 산재보험 근로자수 폴백
                     String addr = cols[4].trim();
                     String bizType = cols[6].trim(); // 업종명
                     String compName = cols[2].trim(); // 사업장명
                     // 기존에 더 큰 근로자수가 있으면 덮어쓰지 않음 (같은 사업자번호로 여러 사업장 가능)
                     insuranceDataMap.merge(bizNo, new String[]{empCount, addr, bizType, compName},
                           (existing, newVal) -> {
                              int existEmp = 0, newEmp = 0;
                              try { existEmp = Integer.parseInt(existing[0]); } catch (Exception e) {}
                              try { newEmp = Integer.parseInt(newVal[0]); } catch (Exception e) {}
                              return (newEmp > existEmp) ? newVal : existing;
                           });
                     loadedCount++;
                  }
               } catch (Exception e) {
                  // 개별 행 파싱 실패 무시
               }
            }
         } catch (Exception e) {
            System.out.println("CSV 파일 읽기 실패: " + csvFile.getName() + " - " + e.getMessage());
         }
      }
      System.out.println("✅ 고용산재보험 CSV 로딩 완료! (총 " + insuranceDataMap.size() + "개 사업장, " + loadedCount + "행 처리)");
   }

   // ====================================================================
   // 🔍 회사명 검색 API - 비즈노에서 관련 회사 목록 전부 반환
   // ====================================================================
   @GetMapping("/search")
   public ResponseEntity<?> searchCompanies(@RequestParam String q) {
      String query = (q != null) ? q.trim() : "";
      if (query.isEmpty()) {
         Map<String, String> err = new HashMap<>();
         err.put("error", "검색어를 입력해주세요.");
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
      }

      RestTemplate restTemplate = new RestTemplate();
      ObjectMapper objectMapper = new ObjectMapper();
      java.util.List<Map<String, String>> resultList = new java.util.ArrayList<>();

      try {
         // 비즈노 gb=3 (상호명 검색) - 최대 10건 반환
         String biznoSearchUrl = "https://www.bizno.net/api/fapi?key=" + BIZNO_API_KEY + "&gb=3&q=" + query
               + "&type=json";
         ResponseEntity<String> searchRes = restTemplate.getForEntity(biznoSearchUrl, String.class);
         JsonNode searchRoot = objectMapper.readTree(searchRes.getBody());

         if (searchRoot.has("items") && searchRoot.get("items").isArray()) {
            for (JsonNode item : searchRoot.get("items")) {
               if (item == null || item.isNull()) continue;
               Map<String, String> company = new HashMap<>();
               company.put("company", item.hasNonNull("company") ? item.get("company").asText().trim() : "");
               String bno = item.hasNonNull("bno") ? item.get("bno").asText().trim() : "";
               company.put("bno", bno);
               company.put("ceo", item.hasNonNull("ceo") ? item.get("ceo").asText().trim() : "");
               company.put("address", item.hasNonNull("address") ? item.get("address").asText().trim() : "");
               company.put("biz_type", item.hasNonNull("biz_type") ? item.get("biz_type").asText().trim() : "");
               company.put("tel", item.hasNonNull("tel") ? item.get("tel").asText().trim() : "");
               company.put("bstt", item.hasNonNull("bstt") ? item.get("bstt").asText().trim() : "");
               // 보험 CSV에서 근로자수 보완
               String cleanBno = bno.replace("-", "").trim();
               if (!cleanBno.isEmpty() && insuranceDataMap.containsKey(cleanBno)) {
                  String[] insData = insuranceDataMap.get(cleanBno);
                  company.put("employeeCount", insData[0]);
                  if (company.get("address").isEmpty() && !insData[1].isEmpty())
                     company.put("address", insData[1]);
                  if (company.get("biz_type").isEmpty() && !insData[2].isEmpty())
                     company.put("biz_type", insData[2]);
               }
               if (!company.get("company").isEmpty()) {
                  resultList.add(company);
               }
            }
         }
      } catch (Exception e) {
         System.out.println("회사명 검색 실패: " + e.getMessage());
      }

      Map<String, Object> response = new HashMap<>();
      response.put("query", query);
      response.put("count", resultList.size());
      response.put("items", resultList);
      return ResponseEntity.ok(response);
   }

   // @RequestParam을 지워도 String 같은 기본 데이터 타입이면 알아서 맵핑 해줍니다.
   @GetMapping("/status") // 상세경로
   public ResponseEntity<?> getCompanyStatus(
         @RequestParam(required = false) String bizNumber,
         @RequestParam(required = false) String companyName) {

      String cleanBizNumber = (bizNumber != null) ? bizNumber.replace("-", "").trim() : "";
      String queryName = (companyName != null) ? companyName.trim() : "";

      RestTemplate restTemplate = new RestTemplate(); // 스프링이 외부와 통신할 수 있게 해주는 도구
      ObjectMapper objectMapper = new ObjectMapper();

      // [핵심] 사업자번호 없이 '회사이름'만 들어왔다면, Bizno API(gb=3)에서 사업자번호를 먼저 역추적합니다!
      if (cleanBizNumber.isEmpty() && !queryName.isEmpty()) {
         try {
            // URI 인코딩 고려 (기본 제공 RestTemplate getForEntity가 자동으로 처리함)
            String searchByNameUrl = "https://www.bizno.net/api/fapi?key=" + BIZNO_API_KEY + "&gb=3&q=" + queryName
                  + "&type=json";
            ResponseEntity<String> searchRes = restTemplate.getForEntity(searchByNameUrl, String.class);
            JsonNode searchRoot = objectMapper.readTree(searchRes.getBody());

            if (searchRoot.has("items") && searchRoot.get("items").isArray() && searchRoot.get("items").size() > 0) {
               JsonNode firstFoundItem = searchRoot.get("items").get(0);
               if (firstFoundItem.hasNonNull("bno")) {
                  bizNumber = firstFoundItem.get("bno").asText(); // 찾은 원본 사업자번호 보존
                  cleanBizNumber = bizNumber.replace("-", "").trim();
               }
            }
         } catch (Exception e) {
            System.out.println("비즈노 상호명 역추적 검색 실패: " + e.getMessage());
         }

         if (cleanBizNumber.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "해당 이름(상호명)으로 등록된 사업자번호를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
         }
      }

      if (cleanBizNumber.isEmpty()) {
         Map<String, String> err = new HashMap<>();
         err.put("error", "사업자등록번호 또는 회사이름 중 하나를 반드시 입력해야 합니다.");
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
      }

      // 공공데이터포털 사업자 상태조회 API 주소
      String ntsUrl = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=" + SERVICE_KEY;
      // 비즈노 API 주소 (이번엔 사업자번호로 상세조회)
      String biznoUrl = "https://www.bizno.net/api/fapi?key=" + BIZNO_API_KEY + "&gb=1&q=" + cleanBizNumber
            + "&type=json";

      // 국세청 API가 요구하는 질문 양식 만들기
      Map<String, List<String>> body = new HashMap<>();
      body.put("b_no", Collections.singletonList(cleanBizNumber));
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

      String realCompanyName = "이름미상"; // 진짜 회사이름을 담을 변수(기본값)

      // 신규 파싱 변수 추가
      String phone = "";
      String email = "";
      String homepage = "";
      Long revenue = null;
      Integer employeeCount = null;
      String address = "";
      String fax = "";
      String industry = "";
      String bsnsYear = "2024"; // DART 매출액 조회 기준년도 (기본 2024, 없으면 2023으로 재시도)
      String ceo = ""; // 대표자명

      try {
         // 1. 국세청 서버로 요청을 보내고 사업자 영업상태 받아오기
         ResponseEntity<String> ntsResponse = restTemplate.postForEntity(ntsUrl, request, String.class);
         JsonNode ntsRoot = objectMapper.readTree(ntsResponse.getBody());
         String b_stt = ""; // 영업상태를 담을 변수

         // 2. 비즈노 서버로 요청을 보내고 상호명(회사 이름) 가져오기
         try {
            ResponseEntity<String> biznoRes = restTemplate.getForEntity(biznoUrl, String.class);
            JsonNode biznoRoot = objectMapper.readTree(biznoRes.getBody());
            System.out.println("[DEBUG] 비즈노 API 응답: " + biznoRes.getBody());

            if (biznoRoot.has("items") && biznoRoot.get("items").isArray() && biznoRoot.get("items").size() > 0) {
               JsonNode firstItem = biznoRoot.get("items").get(0);
               if (firstItem.hasNonNull("company") && !firstItem.get("company").asText().isBlank()) {
                  realCompanyName = firstItem.get("company").asText();
               } else if (firstItem.hasNonNull("company_name") && !firstItem.get("company_name").asText().isBlank()) {
                  realCompanyName = firstItem.get("company_name").asText();
               }

               if (firstItem.hasNonNull("tel") && !firstItem.get("tel").asText().isBlank())
                  phone = firstItem.get("tel").asText().trim();
               if (firstItem.hasNonNull("email") && !firstItem.get("email").asText().isBlank())
                  email = firstItem.get("email").asText().trim();
               if (firstItem.hasNonNull("fax") && !firstItem.get("fax").asText().isBlank())
                  fax = firstItem.get("fax").asText().trim();
               if (firstItem.hasNonNull("address") && !firstItem.get("address").asText().isBlank())
                  address = firstItem.get("address").asText().trim();
               if (firstItem.hasNonNull("biz_type") && !firstItem.get("biz_type").asText().isBlank())
                  industry = firstItem.get("biz_type").asText().trim();
               if (firstItem.hasNonNull("homepage") && !firstItem.get("homepage").asText().isBlank())
                  homepage = validateUrlExistance(firstItem.get("homepage").asText().trim());

            }

            // 첫 번째 접속 (bizno.net) 에서 상호명을 못 찾은 경우 두 번째 API 도메인 (bizno.or.kr) 으로 재시도
            // (Fallback 방어 코드)
            if ("이름미상".equals(realCompanyName)) {
               String fallbackBiznoUrl = "https://api.bizno.or.kr/openapi/v2/company/" + cleanBizNumber + "?apiKey="
                     + BIZNO_API_KEY;
               ResponseEntity<String> fallbackRes = restTemplate.getForEntity(fallbackBiznoUrl, String.class);
               JsonNode fallbackRoot = objectMapper.readTree(fallbackRes.getBody());

               if (fallbackRoot.has("items") && fallbackRoot.get("items").isArray()
                     && fallbackRoot.get("items").size() > 0) {
                  JsonNode fItem = fallbackRoot.get("items").get(0);
                  if (fItem.hasNonNull("company_name")) {
                     realCompanyName = fItem.get("company_name").asText();
                  } else if (fItem.hasNonNull("company")) {
                     realCompanyName = fItem.get("company").asText();
                  }

                  if (phone.isEmpty() && fItem.hasNonNull("tel"))
                     phone = fItem.get("tel").asText();
                  if (email.isEmpty() && fItem.hasNonNull("email"))
                     email = fItem.get("email").asText();
                  if (fax.isEmpty() && fItem.hasNonNull("fax"))
                     fax = fItem.get("fax").asText();
                  if (address.isEmpty() && fItem.hasNonNull("address"))
                     address = fItem.get("address").asText();
                  if (industry.isEmpty() && fItem.hasNonNull("biz_type"))
                     industry = fItem.get("biz_type").asText();
                  if ((homepage == null || homepage.isEmpty()) && fItem.hasNonNull("homepage"))
                     homepage = validateUrlExistance(fItem.get("homepage").asText());
               }
            }
         } catch (Exception e) {
            System.out.println("비즈노 API 호출 실패 (상호명 조회 실패): " + e.getMessage());
         }

         System.out.println(
               "[DEBUG] 비즈노 gb=1 결과 -> 회사명:" + realCompanyName + " 전화:" + phone + " 주소:" + address + " 업종:" + industry);

         // ====================================================================
         // 2.3 비즈노 gb=3 (상호명 검색) - gb=1에서 못 가져온 상세정보(전화,주소,업종 등) 보완
         // ====================================================================
         if (!"이름미상".equals(realCompanyName) && phone.isEmpty() && address.isEmpty()) {
            try {
               String biznoNameUrl = "https://www.bizno.net/api/fapi?key=" + BIZNO_API_KEY + "&gb=3&q="
                     + java.net.URLEncoder.encode(realCompanyName, "UTF-8") + "&type=json";
               ResponseEntity<String> biznoNameRes = restTemplate.getForEntity(biznoNameUrl, String.class);
               JsonNode biznoNameRoot = objectMapper.readTree(biznoNameRes.getBody());
               System.out.println("[DEBUG] 비즈노 gb=3 상세조회 응답: " + biznoNameRes.getBody());

               if (biznoNameRoot.has("items") && biznoNameRoot.get("items").isArray()) {
                  // 사업자번호가 일치하는 항목을 찾아야 함
                  for (JsonNode item : biznoNameRoot.get("items")) {
                     if (item == null || item.isNull())
                        continue;
                     String itemBno = item.hasNonNull("bno") ? item.get("bno").asText().replace("-", "").trim() : "";
                     if (itemBno.equals(cleanBizNumber)) {
                        System.out.println("[DEBUG] 비즈노 gb=3에서 사업자번호 매칭 항목 발견!");
                        if (phone.isEmpty() && item.hasNonNull("tel") && !item.get("tel").asText().isBlank())
                           phone = item.get("tel").asText().trim();
                        if (email.isEmpty() && item.hasNonNull("email") && !item.get("email").asText().isBlank())
                           email = item.get("email").asText().trim();
                        if (fax.isEmpty() && item.hasNonNull("fax") && !item.get("fax").asText().isBlank())
                           fax = item.get("fax").asText().trim();
                        if (address.isEmpty() && item.hasNonNull("address") && !item.get("address").asText().isBlank())
                           address = item.get("address").asText().trim();
                        if (industry.isEmpty() && item.hasNonNull("biz_type")
                              && !item.get("biz_type").asText().isBlank())
                           industry = item.get("biz_type").asText().trim();
                        if ((homepage == null || homepage.isEmpty()) && item.hasNonNull("homepage")
                              && !item.get("homepage").asText().isBlank())
                           homepage = validateUrlExistance(item.get("homepage").asText().trim());
                        if (ceo.isEmpty() && item.hasNonNull("ceo") && !item.get("ceo").asText().isBlank())
                           ceo = item.get("ceo").asText().trim();
                        break;
                     }
                  }
               }
            } catch (Exception e) {
               System.out.println("비즈노 gb=3 상세조회 실패 (무시 가능): " + e.getMessage());
            }
            System.out.println(
                  "[DEBUG] 비즈노 gb=3 보완 후 -> 전화:" + phone + " 주소:" + address + " 업종:" + industry + " 대표:" + ceo);
         }

         // ====================================================================
         // 2.5 DART 연동 (우리가 받아둔 고유번호 메모리 장부를 통해 매출액 추적!)
         // ====================================================================
         String checkName = realCompanyName.replace("(주)", "").replace("주식회사", "").replace(" ", "").trim();
         System.out.println(
               "[DEBUG] DART 매핑 시도: '" + checkName + "' -> 존재여부: " + companyNameToCorpCodeMap.containsKey(checkName));
         if (companyNameToCorpCodeMap.containsKey(checkName)) {
            String corpCode = companyNameToCorpCodeMap.get(checkName);
            System.out.println("[DEBUG] DART 고유번호 발견: " + corpCode);

            // 💰 1) [DART 매출액 조회] - 다양한 계정명 + 보고서/재무제표/연도별 전방위 검색
            String[] reprtCodes = {"11011", "11012", "11014", "11013"}; // 사업보고서, 반기, 3분기, 1분기
            String[] fsDivs = {"CFS", "OFS"}; // 연결재무제표 → 별도재무제표
            // 업종마다 매출을 나타내는 계정명이 다름 (제조업=매출액, 금융=영업수익, 보험=보험료수익 등)
            java.util.Set<String> revenueAccountNames = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                  "매출액", "영업수익", "수익(매출액)", "매출", "순매출액",
                  "보험료수익", "이자수익", "영업수익(매출액)", "순영업수익"));
            String foundReprtCode = null; // 성공한 보고서 코드 기억용
            String[] yearsToTry = {"2024", "2023", "2022"};

            for (String year : yearsToTry) {
               if (revenue != null) break;
               for (String reprtCode : reprtCodes) {
                  if (revenue != null) break;
                  for (String fsDiv : fsDivs) {
                     if (revenue != null) break;
                     String dartFnUrl = "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json?crtfc_key=" + DART_API_KEY
                           + "&corp_code=" + corpCode + "&bsns_year=" + year + "&reprt_code=" + reprtCode + "&fs_div=" + fsDiv;
                     try {
                        ResponseEntity<String> dartRes = restTemplate.getForEntity(dartFnUrl, String.class);
                        JsonNode dartRoot = objectMapper.readTree(dartRes.getBody());
                        if (dartRoot.has("status") && "000".equals(dartRoot.get("status").asText()) && dartRoot.has("list")) {
                           for (JsonNode item : dartRoot.get("list")) {
                              String acctNm = item.has("account_nm") ? item.get("account_nm").asText().trim() : "";
                              if (revenueAccountNames.contains(acctNm)) {
                                 String amt = item.has("thstrm_amount") ? item.get("thstrm_amount").asText().replaceAll(",", "").trim() : "";
                                 if (!amt.isEmpty() && !"-".equals(amt)) {
                                    try {
                                       revenue = Long.parseLong(amt);
                                       foundReprtCode = reprtCode;
                                       bsnsYear = year;
                                       System.out.println("[DEBUG] DART 매출액 발견! 계정명=" + acctNm + " (reprt=" + reprtCode + ", fs=" + fsDiv + ", 년도=" + year + ")");
                                    } catch (NumberFormatException nfe) {
                                       System.out.println("[DEBUG] DART 매출액 숫자변환 실패: '" + amt + "'");
                                    }
                                 }
                                 break;
                              }
                           }
                        }
                     } catch (Exception e) {
                        // 개별 조합 실패는 무시하고 다음 조합 시도
                     }
                  }
               }
            }
            if (revenue == null) System.out.println("[DEBUG] DART 매출액: 모든 계정명/보고서/재무제표/연도 조합에서 찾지 못함");

            // 💡 2) [DART 기업개황 조회] - 비즈노에서 못 가져오는 전화번호, 팩스, 주소, 홈페이지, 업종을 여기서 확보!
            String dartCompanyUrl = "https://opendart.fss.or.kr/api/company.json?crtfc_key=" + DART_API_KEY
                  + "&corp_code=" + corpCode;
            try {
               ResponseEntity<String> dartCompRes = restTemplate.getForEntity(dartCompanyUrl, String.class);
               JsonNode dartComp = objectMapper.readTree(dartCompRes.getBody());
               if (dartComp.has("status") && "000".equals(dartComp.get("status").asText())) {
                  if (phone.isEmpty() && dartComp.hasNonNull("phn_no"))
                     phone = dartComp.get("phn_no").asText().trim();
                  if (fax.isEmpty() && dartComp.hasNonNull("fax_no"))
                     fax = dartComp.get("fax_no").asText().trim();
                  if (address.isEmpty() && dartComp.hasNonNull("adres"))
                     address = dartComp.get("adres").asText().trim();
                  if ((homepage == null || homepage.isEmpty()) && dartComp.hasNonNull("hm_url")) {
                     String dartHp = dartComp.get("hm_url").asText().trim();
                     if (!dartHp.isEmpty())
                        homepage = dartHp;
                  }
                  if (industry.isEmpty() && dartComp.hasNonNull("induty_code"))
                     industry = dartComp.get("induty_code").asText().trim();
                  if (ceo.isEmpty() && dartComp.hasNonNull("ceo_nm"))
                     ceo = dartComp.get("ceo_nm").asText().trim();
               }
            } catch (Exception e) {
               System.out.println("DART 기업개황 확인 실패: " + e.getMessage());
            }

             // 💡 3) [DART 직원현황 조회] - 여러 보고서 코드 순차 시도
             String empReprtCode = (foundReprtCode != null) ? foundReprtCode : "11011";
             String[] empReprtCodes = {empReprtCode, "11011", "11012", "11014", "11013"};
             java.util.Set<String> triedEmpCodes = new java.util.HashSet<>();

             for (String reprtCode : empReprtCodes) {
                if (employeeCount != null || !triedEmpCodes.add(reprtCode)) continue;
                String dartEmpUrl = "https://opendart.fss.or.kr/api/empSttus.json?crtfc_key=" + DART_API_KEY
                      + "&corp_code=" + corpCode + "&bsns_year=" + bsnsYear + "&reprt_code=" + reprtCode;
                try {
                   ResponseEntity<String> dartEmpRes = restTemplate.getForEntity(dartEmpUrl, String.class);
                   JsonNode dartEmpRoot = objectMapper.readTree(dartEmpRes.getBody());
                   if (dartEmpRoot.has("status") && "000".equals(dartEmpRoot.get("status").asText())
                         && dartEmpRoot.has("list")) {
                      int sumAll = 0;
                      int sumHap = 0;
                      for (JsonNode empItem : dartEmpRoot.get("list")) {
                         int rowCount = 0;
                         if (empItem.hasNonNull("rgllbr_co")) {
                            String cnt = empItem.get("rgllbr_co").asText().replaceAll(",", "").trim();
                            if (!cnt.isEmpty() && !"-".equals(cnt))
                               rowCount += Integer.parseInt(cnt);
                         }
                         if (empItem.hasNonNull("cnttk_co")) {
                            String cnt = empItem.get("cnttk_co").asText().replaceAll(",", "").trim();
                            if (!cnt.isEmpty() && !"-".equals(cnt))
                               rowCount += Integer.parseInt(cnt);
                         }
                         sumAll += rowCount;
                         if (empItem.hasNonNull("fo_bbm")) {
                            String dept = empItem.get("fo_bbm").asText().trim();
                            if (dept.equals("합계") || dept.equals("총계") || dept.equals("계")) {
                               sumHap += rowCount;
                            }
                         }
                      }
                      if (sumHap > 0) employeeCount = sumHap;
                      else if (sumAll > 0) employeeCount = sumAll;
                      if (employeeCount != null)
                         System.out.println("[DEBUG] DART 직원수 발견! (reprt=" + reprtCode + ", 년도=" + bsnsYear + ", 인원=" + employeeCount + ")");
                   }
                } catch (Exception e) {
                   // 개별 실패 무시
                }
             }

             // 직원수: 현재 년도 실패 시 이전 년도 재시도
             if (employeeCount == null) {
                String fallbackYear = "2024".equals(bsnsYear) ? "2023" : "2024";
                triedEmpCodes.clear();
                for (String reprtCode : empReprtCodes) {
                   if (employeeCount != null || !triedEmpCodes.add(reprtCode)) continue;
                   String dartEmpUrl = "https://opendart.fss.or.kr/api/empSttus.json?crtfc_key=" + DART_API_KEY
                         + "&corp_code=" + corpCode + "&bsns_year=" + fallbackYear + "&reprt_code=" + reprtCode;
                   try {
                      ResponseEntity<String> dartEmpRes = restTemplate.getForEntity(dartEmpUrl, String.class);
                      JsonNode dartEmpRoot = objectMapper.readTree(dartEmpRes.getBody());
                      if (dartEmpRoot.has("status") && "000".equals(dartEmpRoot.get("status").asText())
                            && dartEmpRoot.has("list")) {
                         int sumAll = 0;
                         int sumHap = 0;
                         for (JsonNode empItem : dartEmpRoot.get("list")) {
                            int rowCount = 0;
                            if (empItem.hasNonNull("rgllbr_co")) {
                               String cnt = empItem.get("rgllbr_co").asText().replaceAll(",", "").trim();
                               if (!cnt.isEmpty() && !"-".equals(cnt))
                                  rowCount += Integer.parseInt(cnt);
                            }
                            if (empItem.hasNonNull("cnttk_co")) {
                               String cnt = empItem.get("cnttk_co").asText().replaceAll(",", "").trim();
                               if (!cnt.isEmpty() && !"-".equals(cnt))
                                  rowCount += Integer.parseInt(cnt);
                            }
                            sumAll += rowCount;
                            if (empItem.hasNonNull("fo_bbm")) {
                               String dept = empItem.get("fo_bbm").asText().trim();
                               if (dept.equals("합계") || dept.equals("총계") || dept.equals("계")) {
                                  sumHap += rowCount;
                               }
                            }
                         }
                         if (sumHap > 0) employeeCount = sumHap;
                         else if (sumAll > 0) employeeCount = sumAll;
                         if (employeeCount != null) {
                            bsnsYear = fallbackYear;
                            System.out.println("[DEBUG] DART 직원수 발견(폴백년도)! (reprt=" + reprtCode + ", 년도=" + fallbackYear + ", 인원=" + employeeCount + ")");
                         }
                      }
                   } catch (Exception e) {
                      // 무시
                   }
                }
             }
             if (employeeCount == null) System.out.println("[DEBUG] DART 직원수: 모든 보고서/년도 조합에서 찾지 못함");
          } else {
             System.out.println("[DEBUG] DART 고유번호 매핑 실패 - 이 회사는 DART에 등록되지 않음");
          }

         // ====================================================================
         // 2.6 국민연금 (NPS) - DART에 없는 비상장 기업용 백업 (주소/업종 보완)
         // ====================================================================
         try {
            java.net.URI npsUri = new java.net.URI(
                  "https://apis.data.go.kr/B552015/NpsBplcInfoInqireService/getbzowrSttusInfoSearch?serviceKey="
                        + SERVICE_KEY + "&bzowr_rgst_no=" + cleanBizNumber + "&pageNo=1&numOfRows=1&type=json");
            ResponseEntity<String> npsRes = restTemplate.getForEntity(npsUri, String.class);
            System.out.println("[DEBUG] 국민연금 API 응답: " + npsRes.getBody());
            JsonNode npsRoot = objectMapper.readTree(npsRes.getBody());
            if (npsRoot.has("response") && npsRoot.get("response").has("body")) {
               JsonNode bodyNode = npsRoot.get("response").get("body");
               if (bodyNode.has("items")) {
                  JsonNode itemsNode = bodyNode.get("items");
                  JsonNode itemNode = null;
                  if (itemsNode.has("item") && itemsNode.get("item").isArray() && itemsNode.get("item").size() > 0)
                     itemNode = itemsNode.get("item").get(0);
                  else if (itemsNode.isArray() && itemsNode.size() > 0)
                     itemNode = itemsNode.get(0);
                  if (itemNode != null) {
                     System.out.println("[DEBUG] NPS 데이터 발견: " + itemNode.toString());
                     // DART에서 인원수를 못 가져왔을 때만 NPS 인원수 사용
                     if (employeeCount == null && itemNode.hasNonNull("nps_vld_cnt"))
                        employeeCount = itemNode.get("nps_vld_cnt").asInt();
                     // 주소는 NPS가 더 최신이므로 항상 덮어쓰기
                     if (itemNode.hasNonNull("addr") && !itemNode.get("addr").asText().isBlank())
                        address = itemNode.get("addr").asText().trim();
                     if (industry.isEmpty() && itemNode.hasNonNull("bzic_nm")
                           && !itemNode.get("bzic_nm").asText().isBlank())
                        industry = itemNode.get("bzic_nm").asText().trim();
                  } else {
                     System.out.println("[DEBUG] NPS items는 있지만 item 내부가 비어있음");
                  }
               } else {
                  System.out.println("[DEBUG] NPS response.body에 items 없음");
               }
            } else {
               System.out.println("[DEBUG] NPS 응답 구조가 예상과 다름");
            }
         } catch (Exception e) {
            System.out.println("국민연금 백업 조회 실패 (무시 가능): " + e.getMessage());
         }

         // ====================================================================
         // 2.7 고용산재보험 CSV 최종 폴백 (200만 사업장 메모리 데이터)
         // ====================================================================
         if (insuranceDataMap.containsKey(cleanBizNumber)) {
            String[] insData = insuranceDataMap.get(cleanBizNumber);
            // insData = [근로자수, 주소, 업종명, 사업장명]
            System.out.println("[DEBUG] 고용산재보험 CSV 매칭! 근로자:" + insData[0] + " 주소:" + insData[1] + " 업종:" + insData[2] + " 사업장:" + insData[3]);
            if (employeeCount == null && !insData[0].isEmpty()) {
               try { employeeCount = Integer.parseInt(insData[0]); } catch (Exception e) {}
            }
            if (address.isEmpty() && !insData[1].isEmpty())
               address = insData[1];
            if (industry.isEmpty() && !insData[2].isEmpty())
               industry = insData[2];
            if ("이름미상".equals(realCompanyName) && !insData[3].isEmpty())
               realCompanyName = insData[3];
         } else {
            System.out.println("[DEBUG] 고용산재보험 CSV에 해당 사업자번호 없음: " + cleanBizNumber);
         }

         System.out.println("[DEBUG] 최종 데이터 -> 회사:" + realCompanyName + " 대표:" + ceo + " 전화:" + phone + " 주소:" + address
               + " 업종:" + industry + " 매출:" + revenue + " 직원:" + employeeCount);

         // 3. 데이터 합치기
         if (ntsRoot.has("data") && ntsRoot.get("data").isArray() && ntsRoot.get("data").size() > 0) {
            ObjectNode dataNode = (ObjectNode) ntsRoot.get("data").get(0);

            dataNode.put("real_company_name", realCompanyName);
            if (!ceo.isEmpty())
               dataNode.put("ceo", ceo);
            if (bizNumber != null)
               dataNode.put("formatted_bno", bizNumber);
            dataNode.put("phone", phone);
            dataNode.put("email", email);
            if (homepage != null)
               dataNode.put("homepage", homepage);
            if (revenue != null) {
               dataNode.put("revenue", revenue);
               dataNode.put("revenueYear", bsnsYear);
            }
            if (employeeCount != null) {
               dataNode.put("employeeCount", employeeCount);
               dataNode.put("employeeYear", bsnsYear);
            }
            if (!address.isEmpty())
               dataNode.put("address", address);
            if (!fax.isEmpty())
               dataNode.put("fax", fax);
            if (!industry.isEmpty())
               dataNode.put("industry", industry);

            // 영업상태 추출
            b_stt = dataNode.get("b_stt").asText();
         }

         // 4. DB에 저장! (사업자번호, 영업상태, 상호명)
         if (!b_stt.isEmpty()) {
            try {
               SearchHistory history = new SearchHistory(bizNumber, b_stt, realCompanyName, phone, email, homepage,
                     revenue, employeeCount, address, fax, industry);
               searchHistoryRepository.save(history);
            } catch (Exception dbError) {
               System.out.println("⚠️ MongoDB 저장 실패 (데이터 반환에는 영향 없음): " + dbError.getMessage());
            }
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

   // URL 생존 여부 확인 유틸리티 (3초 타임아웃)
   private String validateUrlExistance(String urlStr) {
      if (urlStr == null || urlStr.trim().isEmpty())
         return null;
      try {
         if (!urlStr.startsWith("http"))
            urlStr = "http://" + urlStr;
         java.net.HttpURLConnection connection = (java.net.HttpURLConnection) java.net.URI.create(urlStr).toURL()
               .openConnection();
         connection.setRequestMethod("HEAD");
         connection.setConnectTimeout(3000);
         connection.setReadTimeout(3000);

         int responseCode = connection.getResponseCode();
         if (responseCode >= 200 && responseCode <= 399) {
            return urlStr;
         }
      } catch (Exception e) {
         return null;
      }
      return null;
   }
}
