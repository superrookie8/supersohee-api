package com.supersohee.api.article.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.supersohee.api.article.domain.Article;

import java.util.Optional;

public interface ArticleRepository extends MongoRepository<Article, String> {
    

     // 메인 기사: 소스 상관없이 최근 기사 1개
     Optional<Article> findFirstByOrderByPublishedAtDesc();
    
     // 소스별 최신 기사 조회 (페이징: 점프볼/루키별 10개씩) - Page 반환으로 변경
     Page<Article> findBySourceOrderByPublishedAtDesc(String source, Pageable pageable);
   


}
