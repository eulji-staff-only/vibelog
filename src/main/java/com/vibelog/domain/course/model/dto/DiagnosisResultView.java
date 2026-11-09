package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 진단 결과 화면(U-08, API-DGN-04) 데이터.
 * - recommended: 추천 트랙(이름, 단계, 대상, 설명)과 그 트랙의 공개 코스 카드
 * - reasons: 추천 이유 = 방금 고른 보기 중 추천 트랙과 연결된 보기 내용. 응답은 저장하지 않으므로(PL-DGN-001)
 *            제출 직후 flash 로만 오고, 나중에 다시 열면 비어 있다 (그때는 트랙 설명만 보여준다)
 * - otherTracks: 다른 트랙 요약 (트랙 순서대로)
 */
public record DiagnosisResultView(TrackIntroView recommended, List<String> reasons, List<OtherTrack> otherTracks) {

    public record OtherTrack(Long trackId, String name, String target, String description) {
    }
}
