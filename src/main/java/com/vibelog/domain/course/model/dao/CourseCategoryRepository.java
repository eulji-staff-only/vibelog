package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
}
