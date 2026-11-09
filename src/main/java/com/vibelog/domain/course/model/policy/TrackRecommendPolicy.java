package com.vibelog.domain.course.model.policy;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import com.vibelog.domain.course.model.entity.Track;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [TrackRecommendPolicy]
 * 역할 : 진단에서 고른 보기로 추천 트랙 1개를 정한다 (FR-DGN-002, PL-DGN-002).
 * 책임 : - 보기마다 연결된 트랙에 점수(score)가 있다. 고른 보기의 점수를 트랙별로 더해 합계가 가장 높은 트랙을 고른다.
 *        - 합계가 같으면 트랙 순서(sort_order: Starter → Controller → Builder → Explorer)가 앞인 트랙을 고른다.
 *        - DB 를 보지 않는 계산만 한다. 데이터는 DiagnosisService 가 모아서 넘긴다.
 */
@Component
public class TrackRecommendPolicy {

    /* tracks: 전체 트랙 (순서는 상관없다). 고른 보기가 없으면 null */
    public Long recommend(List<DiagnosisOption> chosenOptions, List<Track> tracks) {
        Map<Long, Integer> scoreByTrack = chosenOptions.stream()
                .collect(Collectors.groupingBy(DiagnosisOption::getTrackId, Collectors.summingInt(DiagnosisOption::getScore)));

        return tracks.stream()
                .filter(track -> scoreByTrack.containsKey(track.getTrackId()))
                .max(Comparator.<Track>comparingInt(track -> scoreByTrack.get(track.getTrackId()))
                        .thenComparing(Comparator.comparingInt(Track::getSortOrder).reversed()))
                .map(Track::getTrackId)
                .orElse(null);
    }
}
