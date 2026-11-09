package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /* API-NTF-01. 최신순, 같은 시각이면 ID 큰 것 먼저. Slice 라 다음 쪽이 있는지만 본다 */
    Slice<Notification> findByMemberIdOrderByCreatedAtDescNotificationIdDesc(Long memberId, Pageable pageable);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    /* M-10 배지 획득 축하: 안 읽은 그 종류 알림 중 가장 최근 것 */
    Optional<Notification> findFirstByMemberIdAndNotificationTypeAndReadAtIsNullOrderByNotificationIdDesc(Long memberId, NotificationType type);

    /* API-NTF-03 모두 읽음 */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.memberId = :memberId and n.readAt is null")
    int markAllRead(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /* PL-NTF-002 보관 90일 */
    @Modifying(clearAutomatically = true)
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
