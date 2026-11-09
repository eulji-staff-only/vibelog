package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    /* 트랙 순서(Starter → Controller → Builder → Explorer)대로 (코스 목록 트랙 칩·묶음 API-CRS-02, 트랙 관리 API-ADM-02) */
    List<Track> findAllByOrderBySortOrderAsc();

    /* 이름·순서 중복 검사 (DB uk_track_name·uk_track_sort_order). 수정할 때는 자기 자신을 뺀다 */
    boolean existsByName(String name);

    boolean existsByNameAndTrackIdNot(String name, Long trackId);

    boolean existsBySortOrder(int sortOrder);

    boolean existsBySortOrderAndTrackIdNot(int sortOrder, Long trackId);
}
