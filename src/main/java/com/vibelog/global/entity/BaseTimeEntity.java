package com.vibelog.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
 * [BaseTimeEntity]
 * 역할 : 모든 Entity 가 상속하는 공통 시간 컬럼(created_at, updated_at).
 * 책임 : 저장할 때 created_at, 수정할 때 updated_at 을 JPA 가 자동으로 채운다. (JpaAuditingConfig 가 켜 준다)
 *        Entity 에서 두 값을 직접 넣거나 setter 를 만들지 않는다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
