package com.vibelog.domain.exam.model.policy;

import java.util.List;

/*
 * [ExamStatPolicy]
 * 역할 : 모의고사 응시자 통계 규칙 (PL-EXM-004·008). 오답 분석 리포트(U-16)와 운영자 확장 대시보드가 같은 기준을 쓰도록 한 곳에 둔다.
 * 책임 : - 점수는 학습자마다 1회차 제출 점수만 모은다 (모으는 쪽이 거른다). 운영자·테스트 계정도 모으는 쪽이 뺀다.
 *        - 넣은 응시자가 5명 미만이면 통계를 보여주지 않는다. 평균은 소수 첫째 자리로 반올림한다.
 *        - DB 를 보지 않는 계산만 한다.
 */
public final class ExamStatPolicy {

    public static final int MIN_RESPONDENT_COUNT = 5;

    private ExamStatPolicy() {
    }

    /* 응시자가 충분한가 (5명 이상) */
    public static boolean hasEnoughRespondents(int respondentCount) {
        return respondentCount >= MIN_RESPONDENT_COUNT;
    }

    /* 점수 평균(소수 첫째 자리). 응시자가 5명 미만이면 null */
    public static Double average(List<Integer> scores) {
        return average(scores.stream().mapToLong(Integer::longValue).sum(), scores.size());
    }

    /* 점수 합계·인원수로 평균(소수 첫째 자리). 응시자가 5명 미만이면 null. DB 에서 합계·인원수만 센 경우에 쓴다 (#481) */
    public static Double average(long scoreSum, long respondentCount) {
        if (respondentCount < MIN_RESPONDENT_COUNT) {
            return null;
        }
        return Math.round((double) scoreSum / respondentCount * 10) / 10.0;
    }
}
