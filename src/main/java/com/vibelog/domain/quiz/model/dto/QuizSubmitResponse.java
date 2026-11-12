package com.vibelog.domain.quiz.model.dto;

import java.math.BigDecimal;
import java.util.List;

/*
 * 제출·채점 응답 (API-LSN-06).
 * - next: 통과하면 다음에 갈 곳 (type CONCEPT·EXAM·NONE, url). 통과하지 못하면 type NONE, url 은 수업 화면.
 * - message: 통과해서 열린 것 ("다음 개념이 열렸습니다" 등). 없으면 null.
 * - tagStats: 개념 태그별 오답 비율(%, 정수)과 약한 개념 여부(그 태그 정답률 60% 미만), 문항 순서대로 (FR-LSN-007, PL-LSN-008, #109).
 *   conceptActivityId: 그 태그가 붙은 같은 수업의 사용 중인 개념(순서 앞), 없으면 null — "이 개념 다시 보기" 링크 (#284)
 */
public record QuizSubmitResponse(BigDecimal correctRate, boolean isPassed, List<TagStat> tagStats, Next next,
                                 String message) {

    public record TagStat(String conceptTagName, int wrongRate, boolean weak, Long conceptActivityId) {

        /* 개념 링크 없이 만드는 기존 호출용 */
        public TagStat(String conceptTagName, int wrongRate, boolean weak) {
            this(conceptTagName, wrongRate, weak, null);
        }
    }

    public record Next(String type, String url) {
    }
}
