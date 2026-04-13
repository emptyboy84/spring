/*(DB와 통신하는 창구)*/

package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {
   // 몽고디비가 기본적으로 제공하는 저장(save), 조회(findAll) 기능을 자동으로 사용할 수 있습니다!

}
