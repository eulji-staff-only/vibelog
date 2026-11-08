package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ConceptTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptTagRepository extends JpaRepository<ConceptTag, Long> {
}
