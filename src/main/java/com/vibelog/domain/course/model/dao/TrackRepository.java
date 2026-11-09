package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    /* 트랙 순서(Starter → Controller → Builder → Explorer)대로 (코스 목록 트랙 칩·묶음, API-CRS-02) */
    List<Track> findAllByOrderBySortOrderAsc();
}
