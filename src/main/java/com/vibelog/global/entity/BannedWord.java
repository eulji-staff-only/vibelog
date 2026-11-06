package com.vibelog.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [BannedWord]
 * 역할 : 금칙어 테이블(banned_word)과 매핑된다. (PL-SEC-003, NFR-SEC-009)
 * 책임 : 컬럼은 sql/01-schema.sql 의 banned_word 그대로 둔다.
 *        금칙어 등록·삭제(운영자 기능)는 아직 없어서 만들기 메서드(create)는 그 이슈에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannedWord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bannedWordId;

    @Column(nullable = false, length = 50)
    private String word;
}
