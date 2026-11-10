package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.exam.model.policy.LessonExamStatusChecker;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.ActivityStatus;
import com.vibelog.domain.lesson.model.dto.LessonActivityCard;
import com.vibelog.domain.lesson.model.dto.LessonPageData;
import com.vibelog.domain.lesson.model.dto.LessonPageResult;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.ActivityTag;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.quiz.model.policy.LessonQuizScoreChecker;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [LessonPageService]
 * 역할 : 수업 화면 GET(API-LSN-01)의 데이터를 만든다.
 * 책임 : - 수강 여부·수업 열림 여부부터 본다. 포기한 수강은 수강 안 함으로 본다 (API-ENR-04, 계약 ⑪과 같게).
 *          둘 다 에러 화면이 아니라 "다른 화면 + 토스트"(③)라
 *          LessonPageResult 로 돌려주고 Controller 가 redirect 한다 (BusinessException 을 쓰지 않는다).
 *        - 활동 목록은 개념·퀴즈(activity 테이블) + 모의고사(계약 ②, 활동 테이블에 없는 가상 항목) 순서로 만든다.
 *        - 열린 모의고사 카드에는 제출한 회차가 있을 때 지난 리포트 링크(FR-EXM-006)를 담는다 (계약 ④).
 *        - 개념 카드에는 연관 개념(API-LSN-07)·개념 태그 ID·내 북마크 여부(API-LSN-08)를 함께 담는다 (ConceptService).
 *        - 기본 선택 활동은 "진행 중"인 첫 활동, 없으면 모의고사. query activity 가 있으면 그 활동(또는 "exam")으로 바꾼다.
 *        - 운영자 미리보기(P2, FR-LSN-015): preview 가 true 면 수강·잠김 검사를 하지 않고 모든 활동을 열린 상태로 보여 준다.
 *          기본 선택은 첫 번째 사용 중인 활동(API-ADM-39). 조회만 하며 진행 기록·학습자 점수·북마크는 읽지도 만들지도 않는다.
 *          운영자인지는 Controller 가 판단해서 넘기고, 이 Service 는 보안 컨텍스트를 읽지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPageService {

    private static final String EXAM_QUERY = "exam";

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final ActivityTagRepository activityTagRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonQuizScoreChecker lessonQuizScoreChecker;
    private final ConceptService conceptService;
    private final LessonExamStatusChecker lessonExamStatusChecker;

    public LessonPageResult getPage(Long memberId, Long courseId, Long lessonId, String activityQuery, boolean preview) {
        Lesson lesson = getLesson(lessonId, courseId);
        if (preview) {
            return LessonPageResult.ok(buildPreview(courseId, lesson, activityQuery));
        }

        // 포기한 수강은 수강 안 함과 같다 (계약 ⑪ NONE, PL-ENR-004). 완료한 코스는 복습할 수 있어서 포기 여부만 본다
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId)
                .filter(enrollment -> !enrollment.isDropped());
        if (enrollmentOpt.isEmpty()) {
            return LessonPageResult.notEnrolled();
        }
        Enrollment enrollment = enrollmentOpt.get();

        boolean lessonUnlocked = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lessonId)
                .map(progress -> progress.isUnlocked())
                .orElse(false);
        if (!lessonUnlocked) {
            return LessonPageResult.locked();
        }

        List<Activity> activities = activityRepository
                .findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lessonId);
        Map<Long, ActivityProgress> progressByActivityId = activityProgressRepository
                .findByEnrollmentIdAndActivityIdIn(enrollment.getEnrollmentId(),
                        activities.stream().map(Activity::getActivityId).toList())
                .stream()
                .collect(Collectors.toMap(ActivityProgress::getActivityId, Function.identity()));

        List<LessonActivityCard> cards = new ArrayList<>();
        for (Activity activity : activities) {
            cards.add(toCard(memberId, activity, progressByActivityId.get(activity.getActivityId())));
        }
        cards.add(toExamCard(memberId, enrollment.getEnrollmentId(), lessonId));

        cards = applySelection(cards, activityQuery);

        return LessonPageResult.ok(new LessonPageData(courseId, lessonId, lesson.getTitle(), lesson.getGoal(), cards));
    }

    /* 운영자 미리보기: 활동을 sort_order 순으로 모두 "열린" 카드로 만든다. 학습자 데이터(진행·점수·북마크)는 읽지 않는다 */
    private LessonPageData buildPreview(Long courseId, Lesson lesson, String activityQuery) {
        List<Activity> activities = activityRepository
                .findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lesson.getLessonId());
        List<LessonActivityCard> cards = new ArrayList<>();
        for (Activity activity : activities) {
            cards.add(toPreviewCard(activity));
        }
        cards.add(new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사", ActivityStatus.IN_PROGRESS, false,
                null, null, null, null, null, null, null, null));
        int selectedIndex = requestedIndex(cards, activityQuery);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        cards.set(selectedIndex, cards.get(selectedIndex).select());
        return new LessonPageData(courseId, lesson.getLessonId(), lesson.getTitle(), lesson.getGoal(), cards, true);
    }

    private LessonActivityCard toPreviewCard(Activity activity) {
        ActivityStatus status = ActivityStatus.IN_PROGRESS;
        if (!activity.isConcept()) {
            return new LessonActivityCard(activity.getActivityId(), LessonActivityCard.QUIZ, activity.getTitle(), status,
                    false, null, null, null, null, null, null, null, null);
        }
        List<Long> tagIds = conceptTagIdsOf(activity.getActivityId());
        return new LessonActivityCard(activity.getActivityId(), LessonActivityCard.CONCEPT, activity.getTitle(), status,
                false, ConceptBodyRenderer.render(activity.getBody(), activity.getBodyFormat()), activity.getBodyFormat(),
                conceptTagNamesOf(tagIds), activity.getToolNoteUrl(), null, null, null, null, tagIds, false,
                conceptService.findRelatedConcepts(activity.getActivityId()));
    }

    private LessonActivityCard toCard(Long memberId, Activity activity, ActivityProgress progress) {
        ActivityStatus status = statusOf(progress);
        String kind = activity.isConcept() ? LessonActivityCard.CONCEPT : LessonActivityCard.QUIZ;

        if (status == ActivityStatus.LOCKED) {
            Long previousActivityId = activityRepository
                    .findFirstByLessonIdAndIsActiveTrueAndSortOrderLessThanOrderBySortOrderDesc(
                            activity.getLessonId(), activity.getSortOrder())
                    .map(Activity::getActivityId)
                    .orElse(null);
            return new LessonActivityCard(activity.getActivityId(), kind, activity.getTitle(), status, false,
                    null, null, null, null, null, null, null, previousActivityId);
        }

        if (activity.isConcept()) {
            List<Long> tagIds = conceptTagIdsOf(activity.getActivityId());
            List<String> tags = conceptTagNamesOf(tagIds);
            Long quizActivityId = activityRepository
                    .findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
                            activity.getLessonId(), activity.getSortOrder())
                    .map(Activity::getActivityId)
                    .orElse(null);
            String renderedBody = ConceptBodyRenderer.render(activity.getBody(), activity.getBodyFormat());
            return new LessonActivityCard(activity.getActivityId(), kind, activity.getTitle(), status, false,
                    renderedBody, activity.getBodyFormat(), tags, activity.getToolNoteUrl(), quizActivityId,
                    null, null, null, tagIds, conceptService.isBookmarked(memberId, activity.getActivityId()),
                    conceptService.findRelatedConcepts(activity.getActivityId()));
        }

        BigDecimal bestCorrectRate = lessonQuizScoreChecker.getBestCorrectRate(memberId, activity.getActivityId());
        return new LessonActivityCard(activity.getActivityId(), kind, activity.getTitle(), status, false,
                null, null, null, null, null, bestCorrectRate, null, null);
    }

    /* 계약 ②(ExamLessonProgress.isExamOpen)와 같은 계산이지만, 이 데이터를 전부 가진 lesson이
       같은 조회 안에서 자기 계약 인터페이스를 다시 불러쓰면 그 인터페이스를 모킹하는 다른 테스트
       (예: SecurityConfigTest)가 lessonContractService 빈을 다른 타입으로 바꿔치기할 때 이 조회가
       같이 깨진다. 그래서 여기서는 리포지토리로 직접 계산한다. */
    private LessonActivityCard toExamCard(Long memberId, Long enrollmentId, Long lessonId) {
        boolean open = activityRepository
                .findTopByLessonIdAndActivityTypeAndIsActiveTrueOrderBySortOrderDesc(lessonId, ActivityType.QUIZ)
                .flatMap(lastQuiz -> activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, lastQuiz.getActivityId()))
                .map(ActivityProgress::isCompleted)
                .orElse(false);
        if (open) {
            // 제출한 회차가 있으면 "지난 리포트 다시 보기" 링크 (FR-EXM-006, 계약 ④ 최근 제출 회차)
            Integer attemptNo = lessonExamStatusChecker.getLatestSubmittedAttemptNos(memberId, List.of(lessonId)).get(lessonId);
            String reportUrl = attemptNo == null ? null : "/lessons/" + lessonId + "/exam/report?attempt=" + attemptNo;
            return new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사", ActivityStatus.IN_PROGRESS, false,
                    null, null, null, null, null, null, "/lessons/" + lessonId + "/exam/attempts", null,
                    List.of(), false, List.of(), reportUrl);
        }
        Long previousActivityId = activityRepository
                .findTopByLessonIdAndActivityTypeAndIsActiveTrueOrderBySortOrderDesc(lessonId, ActivityType.QUIZ)
                .map(Activity::getActivityId)
                .orElse(null);
        return new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사", ActivityStatus.LOCKED, false,
                null, null, null, null, null, null, null, previousActivityId);
    }

    private ActivityStatus statusOf(ActivityProgress progress) {
        if (progress == null || !progress.isUnlocked()) {
            return ActivityStatus.LOCKED;
        }
        return progress.isCompleted() ? ActivityStatus.COMPLETED : ActivityStatus.IN_PROGRESS;
    }

    private List<Long> conceptTagIdsOf(Long activityId) {
        return activityTagRepository.findByActivityId(activityId).stream()
                .map(ActivityTag::getConceptTagId)
                .toList();
    }

    private List<String> conceptTagNamesOf(List<Long> conceptTagIds) {
        if (conceptTagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nameByTagId = conceptTagRepository.findAllById(conceptTagIds).stream()
                .collect(Collectors.toMap(ConceptTag::getConceptTagId, ConceptTag::getName));
        return conceptTagIds.stream().map(nameByTagId::get).toList();
    }

    /* 기본값은 "진행 중"인 첫 활동(없으면 모의고사). activity 쿼리가 있고 그 활동을 찾으면 그걸로 바꾼다 */
    private List<LessonActivityCard> applySelection(List<LessonActivityCard> cards, String activityQuery) {
        int selectedIndex = requestedIndex(cards, activityQuery);
        if (selectedIndex < 0) {
            selectedIndex = defaultIndex(cards);
        }
        List<LessonActivityCard> result = new ArrayList<>(cards);
        result.set(selectedIndex, result.get(selectedIndex).select());
        return result;
    }

    private int requestedIndex(List<LessonActivityCard> cards, String activityQuery) {
        if (activityQuery == null || activityQuery.isBlank()) {
            return -1;
        }
        if (EXAM_QUERY.equals(activityQuery)) {
            return indexOfKind(cards, LessonActivityCard.EXAM);
        }
        Long requestedId = parseActivityId(activityQuery);
        if (requestedId == null) {
            return -1;
        }
        for (int i = 0; i < cards.size(); i++) {
            if (requestedId.equals(cards.get(i).activityId())) {
                return i;
            }
        }
        return -1;
    }

    private Long parseActivityId(String activityQuery) {
        try {
            return Long.valueOf(activityQuery);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int indexOfKind(List<LessonActivityCard> cards, String kind) {
        for (int i = 0; i < cards.size(); i++) {
            if (kind.equals(cards.get(i).kind())) {
                return i;
            }
        }
        return -1;
    }

    private int defaultIndex(List<LessonActivityCard> cards) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).status() == ActivityStatus.IN_PROGRESS) {
                return i;
            }
        }
        return cards.size() - 1;
    }

    private Lesson getLesson(Long lessonId, Long courseId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
        if (!lesson.getCourseId().equals(courseId)) {
            throw new BusinessException(LessonErrorCode.LESSON_NOT_FOUND);
        }
        return lesson;
    }
}
