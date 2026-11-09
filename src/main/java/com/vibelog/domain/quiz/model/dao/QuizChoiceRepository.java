package com.vibelog.domain.quiz.model.dao;

import com.vibelog.domain.quiz.model.entity.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {
}
