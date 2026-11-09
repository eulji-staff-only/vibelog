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
 *        - 합계는 long 으로 더한다. 점수는 1~INT 범위라 큰 점수 여러 개를 골라도 넘치지 않게 (admin.md 진단 설정 결정 1, #187).
 *        - DB 를 보지 않는 계산만 한다. 실제 진단(DiagnosisService)과 운영자 테스트 응답(DiagnosisAdminService)이 같이 쓴다.
 */
@Component
public class TrackRecommendPolicy {

    /* tracks: 전체 트랙 (순서는 상관없다). 고른 보기가 없으면 null */
    public Long recommend(List<DiagnosisOption> chosenOptions, List<Track> tracks) {
        Map<Long, Long> scoreByTrack = sumByTrack(chosenOptions);

        return tracks.stream()
                .filter(track -> scoreByTrack.containsKey(track.getTrackId()))
                .max(Comparator.<Track>comparingLong(track -> scoreByTrack.get(track.getTrackId()))
                        .thenComparing(Comparator.comparingInt(Track::getSortOrder).reversed()))
                .map(Track::getTrackId)
                .orElse(null);
    }

    /* 트랙 ID → 고른 보기 점수 합계. 고른 보기가 없는 트랙은 빠진다 (테스트 응답 미리 보기도 쓴다) */
    public Map<Long, Long> sumByTrack(List<DiagnosisOption> chosenOptions) {
        return chosenOptions.stream()
                .collect(Collectors.groupingBy(DiagnosisOption::getTrackId, Collectors.summingLong(DiagnosisOption::getScore)));
    }
}
