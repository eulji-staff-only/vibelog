package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    /* 카테고리 관리 목록, 순서대로 (API-ADM-06) */
    List<CourseCategory> findAllByOrderBySortOrderAscCourseCategoryIdAsc();

    /* 이름 중복 검사 (DB uk_course_category_name, DUPLICATE_NAME). 수정할 때는 자기 자신을 뺀다 */
    boolean existsByName(String name);

    boolean existsByNameAndCourseCategoryIdNot(String name, Long courseCategoryId);

    /* 새 카테고리는 맨 뒤 순서로 만든다 */
    Optional<CourseCategory> findTopByOrderBySortOrderDesc();
}
