package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
