package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ConceptBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConceptBookmarkRepository extends JpaRepository<ConceptBookmark, Long> {

    Optional<ConceptBookmark> findByMemberIdAndActivityId(Long memberId, Long activityId);

    boolean existsByMemberIdAndActivityId(Long memberId, Long activityId);

    /* 북마크한 시각 내림차순, 같으면 나중에 만든 것 먼저 (LessonReader.findBookmarkedConcepts, 마이페이지 북마크 탭) */
    Page<ConceptBookmark> findByMemberIdOrderByCreatedAtDescConceptBookmarkIdDesc(Long memberId, Pageable pageable);
}
