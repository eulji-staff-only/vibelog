package com.vibelog.domain.portfolio.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [GuideStepCheck]
 * 역할 : 스텝 완료 체크 테이블(guide_step_check)과 매핑된다. 회원·스텝당 한 줄 (uk_guide_step_check_member_step).
 * 책임 : 컬럼은 sql/01-schema.sql 의 guide_step_check 그대로 둔다. 체크를 풀면 행을 지운다 (DB 설계서 5-1).
 *        만들기 메서드는 스텝 체크 기능(API-PRT-02)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStepCheck extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guideStepCheckId;

    private Long memberId;

    private Long guideStepId;

    private LocalDateTime checkedAt;
}
