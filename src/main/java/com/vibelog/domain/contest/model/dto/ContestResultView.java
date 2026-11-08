package com.vibelog.domain.contest.model.dto;

import java.util.List;

/*
 * 콘테스트 결과 (API-CTS-05 U-33 상세, API-CTS-01 U-32 결과 보드). 수상작은 순위순.
 * 발표 뒤 작성자가 비공개·삭제한 수상작은 title 이 null → "비공개된 작품", 링크 없음 (contest 명세 결정 14).
 */
public record ContestResultView(Long contestId, String contestMonth, String topic, String description, List<AwardItem> awards) {

    public record AwardItem(int rank, Long portfolioId, String title, String nickname, long votes) {

        public boolean visible() {
            return title != null;
        }
    }
}
