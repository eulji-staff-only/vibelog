package com.vibelog.domain.exam.model.policy;

import com.vibelog.domain.exam.model.dao.ExamAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
 * [ExamLessonStatusChecker]
 * 역할 : 계약 ④ LessonExamStatusChecker 의 exam 영역 구현.
 * 책임 : 수업 ID 묶음을 한 번의 쿼리로 조회한다 (목록 화면에서 수업마다 쿼리가 나가지 않게).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamLessonStatusChecker implements LessonExamStatusChecker {

    private final ExamAttemptRepository examAttemptRepository;

    @Override
    public Map<Long, Integer> getLatestSubmittedAttemptNos(Long memberId, Collection<Long> lessonIds) {
        if (lessonIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : examAttemptRepository.findLatestSubmittedAttemptNos(memberId, lessonIds)) {
            result.put((Long) row[0], (Integer) row[1]);
        }
        return result;
    }
}
