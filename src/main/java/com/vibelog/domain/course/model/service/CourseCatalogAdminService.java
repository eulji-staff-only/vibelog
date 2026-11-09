package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.CourseCategoryRepository;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.AdminCategoryItem;
import com.vibelog.domain.course.model.dto.AdminTrackItem;
import com.vibelog.domain.course.model.dto.CategoryForm;
import com.vibelog.domain.course.model.dto.TrackForm;
import com.vibelog.domain.course.model.entity.CourseCategory;
import com.vibelog.domain.course.model.entity.Track;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [CourseCatalogAdminService]
 * 역할 : 운영자의 트랙·카테고리 관리 (A-02·A-03, API-ADM-02~10, FR-CRS-007·008).
 * 책임 : - 이름은 겹칠 수 없다 → DUPLICATE_NAME (DB uk_track_name·uk_course_category_name). 트랙 순서도 겹칠 수 없다 → SORT_ORDER_TAKEN.
 *        - 코스가 하나라도 있는 트랙·카테고리는 지우지 않는다 → TRACK_HAS_COURSES·CATEGORY_HAS_COURSES (화면 흐름 4장).
 *          코스는 트랙·카테고리를 꼭 가져야 한다 (course.track_id·course_category_id NOT NULL).
 *        - 카테고리 순서는 모든 카테고리 ID 를 새 순서대로 받아 1부터 다시 매긴다. 빠지거나 없는 ID 가 있으면 INVALID_INPUT.
 *        - 운영자인지는 SecurityConfig(/admin/**) 가 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCatalogAdminService {

    private final TrackRepository trackRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final CourseRepository courseRepository;

    /* ---- 트랙 ---- */

    /* A-02 목록: 순서대로, 코스 수와 함께 (트랙은 몇 개뿐이라 트랙마다 센다) */
    public List<AdminTrackItem> getTracks() {
        return trackRepository.findAllByOrderBySortOrderAsc().stream()
                .map(track -> new AdminTrackItem(track.getTrackId(), track.getName(), track.getTarget(), track.getDescription(),
                        track.getSortOrder(), courseRepository.countByTrackId(track.getTrackId())))
                .toList();
    }

    /* 수정 모달을 채울 값 */
    public TrackForm getTrackForm(Long trackId) {
        Track track = getTrack(trackId);
        return TrackForm.of(track.getName(), track.getTarget(), track.getDescription(), track.getSortOrder());
    }

    @Transactional
    public void createTrack(TrackForm form) {
        if (trackRepository.existsByName(form.getName().strip())) {
            throw new BusinessException(CourseErrorCode.DUPLICATE_NAME);
        }
        if (trackRepository.existsBySortOrder(form.getSortOrder())) {
            throw new BusinessException(CourseErrorCode.SORT_ORDER_TAKEN);
        }
        trackRepository.save(Track.create(form.getName().strip(), form.getTarget().strip(), form.getDescription().strip(), form.getSortOrder()));
    }

    @Transactional
    public void updateTrack(Long trackId, TrackForm form) {
        Track track = getTrack(trackId);
        if (trackRepository.existsByNameAndTrackIdNot(form.getName().strip(), trackId)) {
            throw new BusinessException(CourseErrorCode.DUPLICATE_NAME);
        }
        if (trackRepository.existsBySortOrderAndTrackIdNot(form.getSortOrder(), trackId)) {
            throw new BusinessException(CourseErrorCode.SORT_ORDER_TAKEN);
        }
        track.update(form.getName().strip(), form.getTarget().strip(), form.getDescription().strip(), form.getSortOrder());
    }

    @Transactional
    public void deleteTrack(Long trackId) {
        Track track = getTrack(trackId);
        if (courseRepository.countByTrackId(trackId) > 0) {
            throw new BusinessException(CourseErrorCode.TRACK_HAS_COURSES);
        }
        trackRepository.delete(track);
    }

    /* ---- 카테고리 ---- */

    /* A-03 목록: 순서대로, 코스 수와 함께 */
    public List<AdminCategoryItem> getCategories() {
        return courseCategoryRepository.findAllByOrderBySortOrderAscCourseCategoryIdAsc().stream()
                .map(category -> new AdminCategoryItem(category.getCourseCategoryId(), category.getName(), category.getSortOrder(),
                        courseRepository.countByCourseCategoryId(category.getCourseCategoryId())))
                .toList();
    }

    public CategoryForm getCategoryForm(Long courseCategoryId) {
        return CategoryForm.of(getCategory(courseCategoryId).getName());
    }

    /* 새 카테고리는 맨 뒤 순서 */
    @Transactional
    public void createCategory(CategoryForm form) {
        if (courseCategoryRepository.existsByName(form.getName().strip())) {
            throw new BusinessException(CourseErrorCode.DUPLICATE_NAME);
        }
        int nextOrder = courseCategoryRepository.findTopByOrderBySortOrderDesc().map(c -> c.getSortOrder() + 1).orElse(1);
        courseCategoryRepository.save(CourseCategory.create(form.getName().strip(), nextOrder));
    }

    @Transactional
    public void updateCategory(Long courseCategoryId, CategoryForm form) {
        CourseCategory category = getCategory(courseCategoryId);
        if (courseCategoryRepository.existsByNameAndCourseCategoryIdNot(form.getName().strip(), courseCategoryId)) {
            throw new BusinessException(CourseErrorCode.DUPLICATE_NAME);
        }
        category.rename(form.getName().strip());
    }

    @Transactional
    public void deleteCategory(Long courseCategoryId) {
        CourseCategory category = getCategory(courseCategoryId);
        if (courseRepository.countByCourseCategoryId(courseCategoryId) > 0) {
            throw new BusinessException(CourseErrorCode.CATEGORY_HAS_COURSES);
        }
        courseCategoryRepository.delete(category);
    }

    /* API-ADM-10: 모든 카테고리 ID 를 새 순서대로 받아 1, 2, 3… 으로 다시 매긴다 */
    @Transactional
    public void reorderCategories(List<Long> courseCategoryIds) {
        Map<Long, CourseCategory> categories = courseCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(CourseCategory::getCourseCategoryId, Function.identity()));
        boolean sameSet = courseCategoryIds.size() == categories.size()
                && new HashSet<>(courseCategoryIds).size() == courseCategoryIds.size()
                && categories.keySet().containsAll(courseCategoryIds);
        if (!sameSet) {
            throw new BusinessException(CourseErrorCode.INVALID_INPUT);
        }
        for (int i = 0; i < courseCategoryIds.size(); i++) {
            categories.get(courseCategoryIds.get(i)).moveTo(i + 1);
        }
    }

    private Track getTrack(Long trackId) {
        return trackRepository.findById(trackId).orElseThrow(() -> new BusinessException(CourseErrorCode.TRACK_NOT_FOUND));
    }

    private CourseCategory getCategory(Long courseCategoryId) {
        return courseCategoryRepository.findById(courseCategoryId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.CATEGORY_NOT_FOUND));
    }
}
