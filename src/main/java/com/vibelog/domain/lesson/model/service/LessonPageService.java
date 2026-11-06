package com.vibelog.domain.lesson.model.service;

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
import com.vibelog.domain.lesson.model.entity.BodyFormat;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.policy.ExamLessonProgress;
import com.vibelog.domain.quiz.model.policy.LessonQuizScoreChecker;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

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
 * 책임 : - 수강 여부·수업 열림 여부부터 본다. 둘 다 에러 화면이 아니라 "다른 화면 + 토스트"(③)라
 *          LessonPageResult 로 돌려주고 Controller 가 redirect 한다 (BusinessException 을 쓰지 않는다).
 *        - 활동 목록은 개념·퀴즈(activity 테이블) + 모의고사(계약 ②, 활동 테이블에 없는 가상 항목) 순서로 만든다.
 *        - 기본 선택 활동은 "진행 중"인 첫 활동, 없으면 모의고사. query activity 가 있으면 그 활동(또는 "exam")으로 바꾼다.
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
    private final ExamLessonProgress examLessonProgress;
    private final LessonQuizScoreChecker lessonQuizScoreChecker;

    public LessonPageResult getPage(Long memberId, Long courseId, Long lessonId, String activityQuery) {
        Lesson lesson = getLesson(lessonId, courseId);

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByMemberIdAndCourseId(memberId, courseId);
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
        cards.add(toExamCard(memberId, lessonId));

        cards = applySelection(cards, activityQuery);

        return LessonPageResult.ok(new LessonPageData(courseId, lessonId, lesson.getTitle(), lesson.getGoal(), cards));
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
            List<String> tags = conceptTagsOf(activity.getActivityId());
            Long quizActivityId = activityRepository
                    .findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
                            activity.getLessonId(), activity.getSortOrder())
                    .map(Activity::getActivityId)
                    .orElse(null);
            String renderedBody = renderBody(activity.getBody(), activity.getBodyFormat());
            return new LessonActivityCard(activity.getActivityId(), kind, activity.getTitle(), status, false,
                    renderedBody, activity.getBodyFormat(), tags, activity.getToolNoteUrl(), quizActivityId,
                    null, null, null);
        }

        BigDecimal bestCorrectRate = lessonQuizScoreChecker.getBestCorrectRate(memberId, activity.getActivityId());
        return new LessonActivityCard(activity.getActivityId(), kind, activity.getTitle(), status, false,
                null, null, null, null, null, bestCorrectRate, null, null);
    }

    private LessonActivityCard toExamCard(Long memberId, Long lessonId) {
        boolean open = examLessonProgress.isExamOpen(memberId, lessonId);
        if (open) {
            return new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사", ActivityStatus.IN_PROGRESS, false,
                    null, null, null, null, null, null, "/lessons/" + lessonId + "/exam", null);
        }
        Long previousActivityId = activityRepository
                .findTopByLessonIdAndActivityTypeAndIsActiveTrueOrderBySortOrderDesc(lessonId, ActivityType.QUIZ)
                .map(Activity::getActivityId)
                .orElse(null);
        return new LessonActivityCard(null, LessonActivityCard.EXAM, "모의고사", ActivityStatus.LOCKED, false,
                null, null, null, null, null, null, null, previousActivityId);
    }

    /*
     * 개념 본문을 안전한 HTML 로 만든다. 마크다운 파서 라이브러리는 추가하지 않는다 (CLAUDE.md "의존성을
     * 마음대로 추가하지 않는다"). RICH_TEXT 는 이미 HTML 이라고 보고 그대로 쓰고, MARKDOWN 은 태그를
     * 이스케이프한 뒤 줄바꿈만 <br> 로 바꾸는 최소 처리만 한다.
     */
    private String renderBody(String body, BodyFormat bodyFormat) {
        if (body == null) {
            return null;
        }
        if (bodyFormat == BodyFormat.RICH_TEXT) {
            return body;
        }
        return HtmlUtils.htmlEscape(body).replace("\r\n", "\n").replace("\n", "<br>\n");
    }

    private ActivityStatus statusOf(ActivityProgress progress) {
        if (progress == null || !progress.isUnlocked()) {
            return ActivityStatus.LOCKED;
        }
        return progress.isCompleted() ? ActivityStatus.COMPLETED : ActivityStatus.IN_PROGRESS;
    }

    private List<String> conceptTagsOf(Long activityId) {
        List<Long> conceptTagIds = activityTagRepository.findByActivityId(activityId).stream()
                .map(ActivityTag::getConceptTagId)
                .toList();
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
