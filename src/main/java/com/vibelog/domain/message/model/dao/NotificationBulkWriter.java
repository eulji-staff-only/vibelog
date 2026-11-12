package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * [NotificationBulkWriter]
 * 역할 : 공지 알림처럼 여러 회원에게 같은 알림을 한꺼번에 넣는다 (#482).
 * 책임 : - notification_id 가 IDENTITY 라 JPA saveAll 은 회원 수만큼 INSERT 를 한 줄씩 보낸다.
 *          그래서 1,000명씩 묶어 여러 행 INSERT 한 번으로 넣는다 (1만 명 → INSERT 10번).
 *        - 받을 회원은 부르는 쪽이 정한다 (MemberReader.findActiveMemberIds). member 표는 읽지 않는다 (영역 경계).
 *        - JdbcTemplate 은 부르는 Service 의 트랜잭션에 함께 묶인다. created_at·updated_at 은 BaseTimeEntity 처럼 앱 시각으로 넣는다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationBulkWriter {

    private static final int CHUNK_SIZE = 1_000;
    private static final String INSERT = "INSERT INTO notification "
            + "(member_id, notification_type, target_type, target_id, content, created_at, updated_at) VALUES ";
    private static final String ROW = "(?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void insertAll(List<Long> memberIds, NotificationType type, NotificationTargetType targetType,
                          Long targetId, String content) {
        LocalDateTime now = LocalDateTime.now();
        for (int from = 0; from < memberIds.size(); from += CHUNK_SIZE) {
            List<Long> chunk = memberIds.subList(from, Math.min(from + CHUNK_SIZE, memberIds.size()));
            List<Object> args = new ArrayList<>(chunk.size() * 7);
            for (Long memberId : chunk) {
                Collections.addAll(args, memberId, type.name(), targetType.name(), targetId, content, now, now);
            }
            jdbcTemplate.update(INSERT + String.join(", ", Collections.nCopies(chunk.size(), ROW)), args.toArray());
        }
    }
}
