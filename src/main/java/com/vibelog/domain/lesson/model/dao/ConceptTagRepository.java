package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ConceptTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConceptTagRepository extends JpaRepository<ConceptTag, Long> {

    /* 운영자 수업 편집 화면(API-ADM-19, #100)의 개념 태그 목록 */
    List<ConceptTag> findAllByOrderByNameAsc();

    /* 활동에 태그를 붙일 때(API-ADM-21·22, #101) 있으면 그대로 쓰고, 없으면 새로 만든다 */
    Optional<ConceptTag> findByName(String name);
}
