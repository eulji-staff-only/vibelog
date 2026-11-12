package com.vibelog.domain.contest.model.dto;

import java.util.List;

/*
 * 콘테스트 결과 (API-CTS-05 U-33 상세, API-CTS-01 U-32 결과 보드). 수상작은 순위순.
 * 발표 뒤 작성자가 비공개·삭제한 수상작은 title 이 null → "비공개된 작품", 링크 없음 (contest 명세 결정 14).
 * 신고로 숨긴 수상작도 title 이 null 이고 reportHidden → "운영 정책에 따라 숨겨진 콘텐츠입니다" (FR-RPT-003, #256).
 * held 는 수상작이 없고 시상 보류 중이라 "신고 검토로 결과 발표가 미뤄졌습니다" (contest 명세 결정 17).
 */
public record ContestResultView(Long contestId, String contestMonth, String topic, String description, List<AwardItem> awards,
                                boolean held) {

    public record AwardItem(int rank, Long portfolioId, String title, String nickname, long votes, boolean reportHidden) {

        public boolean visible() {
            return title != null;
        }
    }
}
