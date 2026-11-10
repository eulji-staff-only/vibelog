package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityProgressRepository;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.EnrollmentRepository;
import com.vibelog.domain.lesson.model.dao.LessonProgressRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import com.vibelog.domain.lesson.model.entity.ActivityTag;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.BodyFormat;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.domain.lesson.model.entity.Enrollment;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.domain.lesson.model.entity.LessonProgress;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.file.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [LessonActivityAdminService]
 * 역할 : 운영자의 활동 추가·수정·사용 중지·순서 변경과 개념 이미지 올리기 (FR-LSN-011, API-ADM-21~25, #101).
 * 책임 : - PL-LSN-001(개념 뒤에는 퀴즈가 하나씩)은 순서 저장(API-ADM-24)에서만 검사한다. 추가·수정 단계에서는
 *          검사하지 않는다 (명세상 INVALID_ORDER는 순서 저장에만 있고, 전체 공개 가능 여부는 FR-LSN-014가
 *          따로 검사한다). 검사는 사용 중지된 활동을 뺀, 저장하려는 새 순서를 기준으로 한다.
 *        - activity 테이블의 uk_activity_lesson_sort(lesson_id, sort_order)는 사용 중지 활동도 포함해 걸려
 *          있어, 순서를 한 번에 최종 값으로 바꾸면 중간에 값이 겹칠 수 있다. 음수로 먼저 바꿔 플러시한 뒤
 *          최종 순서를 매긴다.
 *        - 태그(tagNames)는 있으면 쓰고 없으면 새 개념 태그를 만든다(find-or-create). 수정 시 기존 태그는
 *          모두 지우고 새로 받은 목록으로 바꾼다.
 *        - 이미지는 global/file/FileStorage(PL-DAT-004, #79)에 그대로 맡긴다. 실패는 FileStorage 가
 *          BusinessException(CommonErrorCode.INVALID_FILE)로 던진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonActivityAdminService {

    private final LessonRepository lessonRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTagRepository activityTagRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ActivityProgressRepository activityProgressRepository;
    private final FileStorage fileStorage;

    /* API-ADM-21. 이미 이 수업을 끝낸 수강자에게는 새 활동을 선택 학습으로 열어 둔다 (PL-ENR-005, #379) */
    @Transactional
    public Long addActivity(Long lessonId, ActivityFormRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.LESSON_NOT_FOUND));
        int nextOrder = activityRepository.findTopByLessonIdOrderBySortOrderDesc(lessonId)
                .map(activity -> activity.getSortOrder() + 1)
                .orElse(1);
        Activity activity = Activity.create(lessonId, request.getActivityType(), request.getTitle(),
                request.getBody(), bodyFormatOf(request), request.getToolNoteUrl(), request.getEstimatedMinutes(),
                passRateOf(request), nextOrder);
        activity = activityRepository.save(activity);
        applyTags(activity.getActivityId(), request.getTagNames());
        openForCompletedLessons(lesson, activity);
        return activity.getActivityId();
    }

    /*
     * PL-ENR-005 "완료한 수업에 활동이 추가되면 새 활동으로 표시하고 선택 학습으로 둔다".
     * 이 수업을 이미 끝낸(lesson_progress.completed_at) 수강자는 새 활동에 들어갈 방법이 없으므로(다음 활동을 여는 곳은 앞 활동을 끝낼 때뿐),
     * 바로 앞 활동을 끝낸 수강자에게는 새 활동을 열린 상태로 만든다. 앞 활동이 아직 안 끝났으면(예: 방금 추가한 개념 뒤에 퀴즈를 더 추가한 경우)
     * 만들지 않는다. 그 개념을 끝낼 때 기존 흐름이 다음 활동을 연다. 수업을 아직 끝내지 못한 수강자는 순서대로 진행하면 닿으므로 손대지 않는다.
     * 새 활동은 수업 완료·진행률·모의고사 열림에 영향을 주지 않는다 (수업 완료는 리포트 확인으로만 기록되고, 모의고사 열림은 LessonExamOpenPolicy가
     * 한 번 열린 것을 다시 잠그지 않는다).
     */
    private void openForCompletedLessons(Lesson lesson, Activity activity) {
        Optional<Activity> previous = activityRepository
                .findFirstByLessonIdAndIsActiveTrueAndSortOrderLessThanOrderBySortOrderDesc(
                        lesson.getLessonId(), activity.getSortOrder());
        for (Enrollment enrollment : enrollmentRepository.findByCourseId(lesson.getCourseId())) {
            Long enrollmentId = enrollment.getEnrollmentId();
            boolean lessonCompleted = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lesson.getLessonId())
                    .map(LessonProgress::isCompleted)
                    .orElse(false);
            if (!lessonCompleted) {
                continue;
            }
            boolean previousDone = previous.isEmpty() || activityProgressRepository
                    .findByEnrollmentIdAndActivityId(enrollmentId, previous.get().getActivityId())
                    .map(ActivityProgress::isCompleted)
                    .orElse(false);
            if (previousDone) {
                activityProgressRepository.save(ActivityProgress.create(enrollmentId, activity.getActivityId(), LocalDateTime.now()));
            }
        }
    }

    /* API-ADM-22. 돌려주는 값은 redirect 할 수업 id 다 (요청에는 activityId만 있고 lessonId는 없다) */
    @Transactional
    public Long editActivity(Long activityId, ActivityFormRequest request) {
        Activity activity = getActivity(activityId);
        activity.edit(request.getTitle(), request.getBody(), bodyFormatOf(request),
                request.getToolNoteUrl(), request.getEstimatedMinutes());
        applyTags(activityId, request.getTagNames());
        return activity.getLessonId();
    }

    /* API-ADM-23. 삭제 대신 사용 중지만 한다 (학습자 진도 보존) */
    @Transactional
    public Long deactivateActivity(Long activityId) {
        Activity activity = getActivity(activityId);
        activity.deactivate();
        return activity.getLessonId();
    }

    /* API-ADM-24 */
    @Transactional
    public void reorderActivities(Long lessonId, List<Long> activityIds) {
        List<Activity> activities = activityRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        Map<Long, Activity> activitiesById = activities.stream()
                .collect(Collectors.toMap(Activity::getActivityId, activity -> activity));
        Set<Long> distinctRequestIds = activityIds == null ? Set.of() : new HashSet<>(activityIds);
        if (activityIds == null || activityIds.isEmpty() || activityIds.size() != activities.size()
                || !activitiesById.keySet().equals(distinctRequestIds)) {
            throw new BusinessException(LessonErrorCode.INVALID_INPUT);
        }

        List<Activity> ordered = activityIds.stream().map(activitiesById::get).toList();
        validateConceptThenQuiz(ordered);

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).changeSortOrder(-(i + 1));
        }
        activityRepository.flush();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).changeSortOrder(i + 1);
        }
    }

    /* API-ADM-25 */
    public String uploadConceptImage(MultipartFile image) {
        return fileStorage.saveImage(image);
    }

    /* PL-LSN-001: 사용 중지된 활동을 뺀 새 순서가 개념·퀴즈·개념·퀴즈... 로 번갈아야 한다 (마지막은 퀴즈) */
    private void validateConceptThenQuiz(List<Activity> ordered) {
        List<Activity> active = ordered.stream().filter(Activity::isActive).toList();
        for (int i = 0; i < active.size(); i++) {
            ActivityType expected = i % 2 == 0 ? ActivityType.CONCEPT : ActivityType.QUIZ;
            if (active.get(i).getActivityType() != expected) {
                throw new BusinessException(LessonErrorCode.INVALID_ORDER);
            }
        }
        if (active.size() % 2 != 0) {
            // 짝이 맞지 않으면(마지막이 개념으로 끝남) 그 개념 뒤에 퀴즈가 없다는 뜻이다
            throw new BusinessException(LessonErrorCode.INVALID_ORDER);
        }
    }

    private BodyFormat bodyFormatOf(ActivityFormRequest request) {
        return request.getBody() == null || request.getBody().isBlank() ? null : BodyFormat.MARKDOWN;
    }

    /* 퀴즈는 통과 기준 50%로 시작한다 (PL-LSN-002). 개념은 pass_rate를 쓰지 않는다 */
    private Integer passRateOf(ActivityFormRequest request) {
        return request.getActivityType() == ActivityType.QUIZ ? 50 : null;
    }

    private void applyTags(Long activityId, List<String> tagNames) {
        activityTagRepository.deleteByActivityId(activityId);
        activityTagRepository.flush(); // 삭제를 먼저 반영해야 같은 태그를 다시 넣어도 유니크 제약에 안 걸린다
        if (tagNames == null) {
            return;
        }
        tagNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::strip)
                .distinct()
                .forEach(name -> {
                    ConceptTag tag = conceptTagRepository.findByName(name)
                            .orElseGet(() -> conceptTagRepository.save(ConceptTag.create(name)));
                    activityTagRepository.save(ActivityTag.create(activityId, tag.getConceptTagId()));
                });
    }

    private Activity getActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND));
    }
}
