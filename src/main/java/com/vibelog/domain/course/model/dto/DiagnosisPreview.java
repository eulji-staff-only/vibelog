package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 진단 설정의 테스트 응답 미리 보기 (API-ADM-45, #187). 저장하지 않는다.
 * 실제 진단과 같은 계산(TrackRecommendPolicy)으로 고른 추천 트랙과, 트랙 순서대로 트랙별 점수 합계.
 */
public record DiagnosisPreview(Long recommendedTrackId, String recommendedTrackName, List<TrackScore> scores) {

    public record TrackScore(Long trackId, String trackName, long score) {
    }
}
