/*어떤 데이터를 저장할지 정하는 설계도*/

package com.example.demo;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "search_history") // 몽고디비 컬렉션 이름
public class SearchHistory {
   @Id
   private String id;
   private String bizNumber;// 사업자등록번호
   private String status;// 사업자상태
   private LocalDateTime searchDate;// 검색한시간
   private String companyName;// 회사이름

   /* constructor */
   public SearchHistory(String bizNumber, String status, String companyName) {
      this.bizNumber = bizNumber;
      this.status = status;
      this.searchDate = LocalDateTime.now();// 저장할때 현재시간 자동입력
      this.companyName = companyName;
   }

}
