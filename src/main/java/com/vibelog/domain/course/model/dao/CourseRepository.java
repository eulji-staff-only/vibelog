package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
