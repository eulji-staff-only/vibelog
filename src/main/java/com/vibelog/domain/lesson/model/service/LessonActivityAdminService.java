package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptTagRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.ActivityFormRequest;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityTag;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import com.vibelog.domain.lesson.model.entity.BodyFormat;
import com.vibelog.domain.lesson.model.entity.ConceptTag;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.file.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final FileStorage fileStorage;

    /* API-ADM-21 */
    @Transactional
    public Long addActivity(Long lessonId, ActivityFormRequest request) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new BusinessException(LessonErrorCode.LESSON_NOT_FOUND);
        }
        int nextOrder = activityRepository.findTopByLessonIdOrderBySortOrderDesc(lessonId)
                .map(activity -> activity.getSortOrder() + 1)
                .orElse(1);
        Activity activity = Activity.create(lessonId, request.getActivityType(), request.getTitle(),
                request.getBody(), bodyFormatOf(request), request.getToolNoteUrl(), request.getEstimatedMinutes(),
                null, nextOrder);
        activity = activityRepository.save(activity);
        applyTags(activity.getActivityId(), request.getTagNames());
        return activity.getActivityId();
    }

    /* API-ADM-22. 돌려주는 값은 redirect 할 수업 id 다 (요청에는 activityId만 있고 lessonId는 없다) */
    @Transactional
    public Long editActivity(Long activityId, ActivityFormRequest request) {
        Activity activity = getActivity(activityId);
        activity.edit(request.getActivityType(), request.getTitle(), request.getBody(), bodyFormatOf(request),
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
