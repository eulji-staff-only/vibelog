package com.vibelog.domain.course.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [DiagnosisOption]
 * 역할 : 진단 보기 테이블(diagnosis_option)과 매핑된다. 문항의 보기 하나이고, 고르면 연결된 트랙에 점수가 쌓인다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 diagnosis_option 그대로 둔다. 문항·트랙은 같은 영역이지만 ID 로만 가진다.
 *        트랙 점수 합계로 추천 트랙을 정하는 규칙은 TrackRecommendPolicy (#99, PL-DGN-001·002).
 *        보기 내용은 바꾸지 않는다. 내용이 바뀌면 진단 설정이 지우고 새 보기(새 ID)로 만든다 — 고치기 전 설문으로 제출한 답이
 *        고르지 않은 새 문구로 추천 이유에 보이지 않게 (API-ADM-43, #187). 트랙·점수·순서만 그 자리에서 바꾼다 (change).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diagnosisOptionId;

    private Long diagnosisQuestionId;

    private Long trackId;

    private String content;

    private int score;

    private int sortOrder;

    /* 보기 추가·내용이 바뀐 보기 다시 만들기 (API-ADM-42·43) */
    public static DiagnosisOption create(Long diagnosisQuestionId, Long trackId, String content, int score, int sortOrder) {
        DiagnosisOption option = new DiagnosisOption();
        option.diagnosisQuestionId = diagnosisQuestionId;
        option.content = content;
        option.change(trackId, score, sortOrder);
        return option;
    }

    /* 내용이 그대로인 보기의 트랙·점수·순서만 바꾼다 (API-ADM-43) */
    public void change(Long trackId, int score, int sortOrder) {
        this.trackId = trackId;
        this.score = score;
        this.sortOrder = sortOrder;
    }
}
