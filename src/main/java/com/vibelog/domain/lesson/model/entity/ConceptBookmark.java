package com.vibelog.domain.lesson.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ConceptBookmark]
 * 역할 : 개념 북마크 테이블(concept_bookmark)과 매핑된다. 회원이 북마크한 개념 활동 하나가 한 행이다 (FR-LSN-004).
 * 책임 : 컬럼은 sql/01-schema.sql 의 concept_bookmark 그대로 둔다. 같은 회원·활동은 한 행만 둔다
 *        (uk_concept_bookmark_member_activity). 북마크한 시각은 created_at 이다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConceptBookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conceptBookmarkId;

    private Long memberId;

    private Long activityId;

    private ConceptBookmark(Long memberId, Long activityId) {
        this.memberId = memberId;
        this.activityId = activityId;
    }

    public static ConceptBookmark create(Long memberId, Long activityId) {
        return new ConceptBookmark(memberId, activityId);
    }
}
