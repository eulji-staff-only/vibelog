package com.vibelog.domain.lesson.model.service;

import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.exception.LessonErrorCode;
import com.vibelog.domain.lesson.model.dao.ActivityRepository;
import com.vibelog.domain.lesson.model.dao.ActivityTagRepository;
import com.vibelog.domain.lesson.model.dao.ConceptBookmarkRepository;
import com.vibelog.domain.lesson.model.dao.ConceptRelationRepository;
import com.vibelog.domain.lesson.model.dao.LessonRepository;
import com.vibelog.domain.lesson.model.dto.BookmarkResult;
import com.vibelog.domain.lesson.model.dto.ConceptSearchItem;
import com.vibelog.domain.lesson.model.dto.RelatedConcept;
import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityTag;
import com.vibelog.domain.lesson.model.entity.ConceptBookmark;
import com.vibelog.domain.lesson.model.entity.ConceptRelation;
import com.vibelog.domain.lesson.model.entity.Lesson;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [ConceptService]
 * 역할 : 개념 활동에 붙는 P2 기능 셋을 맡는다 - 연관 개념(API-LSN-07), 북마크 토글(API-LSN-08), 태그 검색(API-LSN-09).
 * 책임 : - "보여줘도 되는 개념"은 사용 중인 개념 활동 + 공개 수업 + 공개 코스일 때만이다. 연관 개념과 태그 검색이 같은 기준을 쓴다
 *          (비공개 코스 제목이 새지 않게, lesson.md 개념 태그 검색 설명). 코스 공개 여부·제목은 course 의 CourseReader 로만 받는다.
 *        - 북마크는 있으면 지우고 없으면 만든다. 개념이 아니거나 없는 활동은 ACTIVITY_NOT_FOUND 를 던진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConceptService {

    private final ActivityRepository activityRepository;
    private final ActivityTagRepository activityTagRepository;
    private final ConceptBookmarkRepository conceptBookmarkRepository;
    private final ConceptRelationRepository conceptRelationRepository;
    private final LessonRepository lessonRepository;
    private final CourseReader courseReader;

    /*
     * API-LSN-08: 있으면 지우고 없으면 만든다 (멱등 아님, 누를 때마다 반대로).
     * 만들 때만 "보여줘도 되는 개념"(사용 중 + 공개 수업 + 공개 코스)인지 확인한다. 숫자 ID 를 직접 넣어 비공개 코스·수업의
     * 개념을 북마크해 마이페이지 북마크 탭으로 제목이 새는 것을 막는다 (없는 활동과 같은 ACTIVITY_NOT_FOUND 로 존재를 숨긴다).
     * 이미 있는 북마크는 나중에 비공개가 되어도 항상 해제할 수 있다.
     */
    @Transactional
    public BookmarkResult toggleBookmark(Long memberId, Long activityId) {
        Activity concept = activityRepository.findById(activityId)
                .filter(Activity::isConcept)
                .orElseThrow(() -> new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND));

        Optional<ConceptBookmark> existing = conceptBookmarkRepository.findByMemberIdAndActivityId(memberId, activityId);
        if (existing.isPresent()) {
            conceptBookmarkRepository.delete(existing.get());
            return new BookmarkResult(false);
        }
        if (!visibleItems(List.of(concept)).containsKey(activityId)) {
            throw new BusinessException(LessonErrorCode.ACTIVITY_NOT_FOUND);
        }
        conceptBookmarkRepository.save(ConceptBookmark.create(memberId, activityId));
        return new BookmarkResult(true);
    }

    public boolean isBookmarked(Long memberId, Long activityId) {
        return conceptBookmarkRepository.existsByMemberIdAndActivityId(memberId, activityId);
    }

    /* API-LSN-09: conceptTagId 가 없으면 빈 목록. 코스 → 수업 → 활동 순서(sort_order)로 돌려준다 */
    public List<ConceptSearchItem> searchByTag(Long conceptTagId) {
        if (conceptTagId == null) {
            return List.of();
        }
        List<Long> activityIds = activityTagRepository.findByConceptTagId(conceptTagId).stream()
                .map(ActivityTag::getActivityId)
                .toList();
        List<Activity> concepts = activityRepository.findAllById(activityIds).stream()
                .filter(Activity::isConcept)
                .toList();
        Map<Long, ConceptSearchItem> visible = visibleItems(concepts);

        Map<Long, Lesson> lessons = lessonsOf(concepts);
        return concepts.stream()
                .filter(activity -> visible.containsKey(activity.getActivityId()))
                .sorted(Comparator
                        .comparing((Activity activity) -> lessons.get(activity.getLessonId()).getCourseId())
                        .thenComparing(activity -> lessons.get(activity.getLessonId()).getSortOrder())
                        .thenComparing(Activity::getSortOrder))
                .map(activity -> visible.get(activity.getActivityId()))
                .toList();
    }

    /* API-LSN-07: 개념 하나의 연관 개념을 concept_relation.sort_order 순으로. 보여줄 수 없는 개념은 뺀다 */
    public List<RelatedConcept> findRelatedConcepts(Long sourceActivityId) {
        List<ConceptRelation> relations = conceptRelationRepository.findBySourceActivityIdOrderBySortOrderAsc(sourceActivityId);
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Activity> concepts = activityRepository.findAllById(
                        relations.stream().map(ConceptRelation::getRelatedActivityId).toList()).stream()
                .filter(Activity::isConcept)
                .toList();
        Map<Long, ConceptSearchItem> visible = visibleItems(concepts);
        return relations.stream()
                .map(relation -> visible.get(relation.getRelatedActivityId()))
                .filter(Objects::nonNull)
                .map(item -> new RelatedConcept(item.activityId(), item.title(), item.courseId(), item.lessonId()))
                .toList();
    }

    private Map<Long, Lesson> lessonsOf(Collection<Activity> activities) {
        return lessonRepository.findAllById(activities.stream().map(Activity::getLessonId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Lesson::getLessonId, Function.identity()));
    }

    /* 사용 중인 개념 활동 중 공개 수업·공개 코스에 속한 것만, 활동 ID → 화면 값으로 */
    private Map<Long, ConceptSearchItem> visibleItems(Collection<Activity> concepts) {
        Map<Long, Lesson> publicLessons = lessonsOf(concepts).entrySet().stream()
                .filter(entry -> entry.getValue().isPublic())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<Long> publicCourseIds = courseReader.getPublicCourseIds(
                publicLessons.values().stream().map(Lesson::getCourseId).collect(Collectors.toSet()));
        Map<Long, String> courseTitles = courseReader.getCourseTitles(publicCourseIds);

        return concepts.stream()
                .filter(Activity::isActive)
                .filter(activity -> {
                    Lesson lesson = publicLessons.get(activity.getLessonId());
                    return lesson != null && publicCourseIds.contains(lesson.getCourseId());
                })
                .collect(Collectors.toMap(Activity::getActivityId, activity -> {
                    Lesson lesson = publicLessons.get(activity.getLessonId());
                    return new ConceptSearchItem(activity.getActivityId(), activity.getTitle(), lesson.getCourseId(),
                            lesson.getLessonId(), courseTitles.get(lesson.getCourseId()), lesson.getTitle());
                }));
    }
}
