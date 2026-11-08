package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.exam.model.dto.AdminExamQuestionItem;
import com.vibelog.domain.exam.model.service.ExamReader;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.AdminActivityItem;
import com.vibelog.domain.lesson.model.dto.ConceptTagOption;
import com.vibelog.domain.lesson.model.dto.LessonEditView;
import com.vibelog.domain.lesson.model.dto.LessonFormRequest;
import com.vibelog.domain.lesson.model.dto.LessonPublishStatus;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.domain.quiz.model.service.QuizReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [LessonAdminService]
 * 역할 : 운영자의 수업 만들기·순서·편집 (FR-LSN-011, API-ADM-17~20, #100).
 * 책임 : - 코스 존재 확인은 CourseReader(#100이 추가한 existsById), 코스 이름 표시는 CourseReader.getCourseTitles로 받는다.
 *        - 활동 추가·수정·순서 변경(API-ADM-21~25)과 퀴즈 문항(API-ADM-26~28)·모의고사 문항(API-ADM-29~31) 등록·수정은
 *          여기서 만들지 않는다 (각각 #101, FR-LSN-012, FR-EXM-007). 편집 화면은 그 결과를 읽기만 한다.
 *        - 퀴즈 문항은 quiz_question Entity가 아직 없어(quiz 영역 기반 작업 전) 목록을 만들지 않는다
 *          (08 문서 "안 멈추는 규칙 1-4": 받는 쪽 Entity가 없으면 화면에 안내만 둔다).
 *        - 수업 공개·비공개(API-ADM-32·33, FR-LSN-014, PL-LSN-003, PL-ENR-005, #118)도 여기서 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonAdminService {

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTagRepository activityTagRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseReader courseReader;
    private final ExamReader examReader;
    private final QuizReader quizReader;

    /* API-ADM-17. 새 수업은 그 코스의 마지막 순서 다음으로, 항상 비공개로 만든다 */
    @Transactional
    public Long create(Long courseId, LessonFormRequest request) {
        if (!courseReader.existsById(courseId)) {
            throw new BusinessException(LessonErrorCode.COURSE_NOT_FOUND);
        }
        int nextOrder = lessonRepository.findTopByCourseIdOrderBySortOrderDesc(courseId)
                .map(lesson -> lesson.getSortOrder() + 1)
                .orElse(1);
        Lesson lesson = Lesson.create(courseId, request.getTitle(), request.getIntro(), request.getGoal(),
                request.getContents(), request.getEstimatedMinutes(), nextOrder);
        return lessonRepository.save(lesson).getLessonId();
    }

    /*
     * API-ADM-18. 보낸 수업 순서 목록이 그 코스의 수업과 1:1로 맞을 때만 저장한다
     * (빠짐·중복·다른 코스의 수업 섞임을 모두 INVALID_INPUT으로 막는다).
     */
    @Transactional
    public void reorder(Long courseId, List<Long> lessonIds) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        Map<Long, Lesson> lessonsById = lessons.stream()
                .collect(Collectors.toMap(Lesson::getLessonId, lesson -> lesson));
        Set<Long> distinctRequestIds = lessonIds == null ? Set.of() : new HashSet<>(lessonIds);
        if (lessonIds == null || lessonIds.isEmpty() || lessonIds.size() != lessons.size()
                || !lessonsById.keySet().equals(distinctRequestIds)) {
            throw new BusinessException(LessonErrorCode.INVALID_INPUT);
        }
        int order = 1;
        for (Long lessonId : lessonIds) {
            lessonsById.get(lessonId).changeSortOrder(order++);
        }
    }

    /* API-ADM-19. tab 은 activities(기본)·exam 만 받고, 그 외 값은 activities로 본다 */
    public LessonEditView getEditView(Long lessonId, String tab) {
        Lesson lesson = getLesson(lessonId);
        String courseTitle = courseReader.getCourseTitles(List.of(lesson.getCourseId()))
                .getOrDefault(lesson.getCourseId(), "코스 " + lesson.getCourseId());
        List<ConceptTag> conceptTagEntities = conceptTagRepository.findAllByOrderByNameAsc();
        Map<Long, String> conceptTagNamesById = conceptTagEntities.stream()
                .collect(Collectors.toMap(ConceptTag::getConceptTagId, ConceptTag::getName));
        List<AdminActivityItem> activities = activityRepository.findByLessonIdOrderBySortOrderAsc(lessonId).stream()
                .map(activity -> toAdminActivityItem(activity, conceptTagNamesById))
                .toList();
        List<ConceptTagOption> conceptTags = conceptTagEntities.stream()
                .map(tag -> new ConceptTagOption(tag.getConceptTagId(), tag.getName()))
                .toList();
        String activeTab = "exam".equals(tab) ? "exam" : "activities";
        return new LessonEditView(lesson.getLessonId(), lesson.getCourseId(), courseTitle, lesson.getTitle(),
                lesson.isPublic(), activeTab, activities, conceptTags, examReader.getQuestionsOf(lessonId),
                checkPublishStatus(lessonId));
    }

    /* A-06 수업 정보 폼에 채울 값 (GET, 그리고 수정 실패로 같은 화면을 다시 보여줄 때는 쓰지 않는다) */
    public LessonFormRequest getForm(Long lessonId) {
        return LessonFormRequest.from(getLesson(lessonId));
    }

    /* API-ADM-20 */
    @Transactional
    public void update(Long lessonId, LessonFormRequest request) {
        Lesson lesson = getLesson(lessonId);
        lesson.edit(request.getTitle(), request.getIntro(), request.getGoal(), request.getContents(),
                request.getEstimatedMinutes());
    }

    /* API-ADM-32. 그 수업이 속한 코스를 수강 중이면서 아직 완료·포기 안 한 학습자 수 (경고 모달) */
    public int getActiveLearnerCount(Long lessonId) {
        Lesson lesson = getLesson(lessonId);
        return (int) enrollmentRepository.countByCourseIdAndCompletedAtIsNullAndDroppedAtIsNull(lesson.getCourseId());
    }

    /*
     * API-ADM-33. 공개하면 필수 항목을 검사하고(PL-LSN-003), 통과하면 기존 수강의 lesson_progress를 추가한다
     * (PL-ENR-005). 비공개는 검사 없이 바로 바꾼다 (영향 학습자 수 경고는 화면에서 API-ADM-32로 미리 확인한다).
     * 돌려주는 코스 id는 Controller가 성공 시 이동할 코스 상세(A-05) 주소에 쓴다.
     */
    @Transactional
    public Long changeVisibility(Long lessonId, boolean isPublic) {
        Lesson lesson = getLesson(lessonId);
        if (isPublic) {
            if (!lesson.isPublic()) {
                LessonPublishStatus status = checkPublishStatus(lessonId);
                if (!status.publishable()) {
                    throw new BusinessException(LessonErrorCode.LESSON_NOT_PUBLISHABLE);
                }
                lesson.publish();
                addProgressForExistingEnrollments(lesson);
            }
        } else {
            lesson.unpublish();
        }
        return lesson.getCourseId();
    }

    /*
     * FR-LSN-014, PL-LSN-003. 공개 가능 조건 네 가지를 검사해 빠진 항목을 모은다.
     * - 개념 뒤 퀴즈: 사용 중인 활동만 봤을 때 개념·퀴즈가 번갈아 오고 마지막은 퀴즈로 끝나야 한다
     *   (#101 LessonActivityAdminService.validateConceptThenQuiz와 같은 규칙).
     * - 퀴즈 3~5문항 · 정답: 퀴즈 활동마다 사용 중인 문항 수(QuizReader)가 3~5개여야 한다. 문항 등록
     *   기능(#102) 전이라 "정답"은 문항이 1개 이상 있으면 있다고 본다.
     * - 모의고사 문항: 사용 중인 문항이 1개 이상 있어야 한다 (ExamReader).
     */
    public LessonPublishStatus checkPublishStatus(Long lessonId) {
        List<Activity> activeActivities = activityRepository.findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(lessonId);
        List<String> missing = new ArrayList<>();
        if (!isConceptThenQuizOrder(activeActivities)) {
            missing.add("개념 뒤 퀴즈");
        }

        List<Activity> quizActivities = activeActivities.stream().filter(Activity::isQuiz).toList();
        Map<Long, Integer> questionCountByActivityId = quizActivities.stream()
                .collect(Collectors.toMap(Activity::getActivityId, a -> quizReader.countActiveQuestionsOf(a.getActivityId())));
        boolean quizCountOk = !quizActivities.isEmpty()
                && questionCountByActivityId.values().stream().allMatch(count -> count >= 3 && count <= 5);
        if (!quizCountOk) {
            missing.add("퀴즈 3~5문항");
        }
        boolean hasAnswers = !quizActivities.isEmpty()
                && questionCountByActivityId.values().stream().allMatch(count -> count >= 1);
        if (!hasAnswers) {
            missing.add("정답");
        }

        boolean hasExamQuestion = examReader.getQuestionsOf(lessonId).stream().anyMatch(AdminExamQuestionItem::active);
        if (!hasExamQuestion) {
            missing.add("모의고사 문항");
        }

        return new LessonPublishStatus(missing.isEmpty(), missing);
    }

    private boolean isConceptThenQuizOrder(List<Activity> activeActivities) {
        if (activeActivities.isEmpty()) {
            return false;
        }
        for (int i = 0; i < activeActivities.size(); i++) {
            ActivityType expected = i % 2 == 0 ? ActivityType.CONCEPT : ActivityType.QUIZ;
            if (activeActivities.get(i).getActivityType() != expected) {
                return false;
            }
        }
        return activeActivities.size() % 2 == 0;
    }

    /* PL-ENR-005. 아직 진도가 없는 수강에만 잠긴 채로 새로 만든다 (EnrollmentService가 두 번째 이후 수업을 만드는 방식과 같다) */
    private void addProgressForExistingEnrollments(Lesson lesson) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(lesson.getCourseId());
        for (Enrollment enrollment : enrollments) {
            boolean alreadyExists = lessonProgressRepository
                    .findByEnrollmentIdAndLessonId(enrollment.getEnrollmentId(), lesson.getLessonId())
                    .isPresent();
            if (!alreadyExists) {
                lessonProgressRepository.save(LessonProgress.create(enrollment.getEnrollmentId(), lesson.getLessonId(), null));
            }
        }
    }

    private AdminActivityItem toAdminActivityItem(Activity activity, Map<Long, String> conceptTagNamesById) {
        List<String> tagNames = activityTagRepository.findByActivityId(activity.getActivityId()).stream()
                .map(tag -> conceptTagNamesById.get(tag.getConceptTagId()))
                .filter(name -> name != null)
                .toList();
        return new AdminActivityItem(activity.getActivityId(), activity.getActivityType(), activity.getTitle(),
                activity.getBody(), activity.getToolNoteUrl(), activity.getEstimatedMinutes(), tagNames,
                activity.getSortOrder(), activity.isActive());
    }

    private Lesson getLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
    }
}
