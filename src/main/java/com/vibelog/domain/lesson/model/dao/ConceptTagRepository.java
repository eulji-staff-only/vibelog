package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ConceptTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConceptTagRepository extends JpaRepository<ConceptTag, Long> {

    /* 운영자 수업 편집 화면(API-ADM-19, #100)의 개념 태그 목록 */
    List<ConceptTag> findAllByOrderByNameAsc();
}
