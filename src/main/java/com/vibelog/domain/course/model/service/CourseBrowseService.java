package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.CourseCategoryRepository;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.DiagnosisResultRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.CategoryChip;
import com.vibelog.domain.course.model.dto.CourseCard;
import com.vibelog.domain.course.model.dto.CourseDetailView;
import com.vibelog.domain.course.model.dto.CourseListView;
import com.vibelog.domain.course.model.dto.TrackChip;
import com.vibelog.domain.course.model.dto.TrackCourseGroup;
import com.vibelog.domain.course.model.dto.TrackIntroView;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.course.model.entity.CourseCategory;
import com.vibelog.domain.course.model.entity.DiagnosisStatus;
import com.vibelog.domain.course.model.entity.Track;
import com.vibelog.domain.lesson.model.policy.CourseLessonInfo;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [CourseBrowseService]
 * 역할 : 학습자가 트랙·코스를 둘러보는 화면의 데이터를 만든다. 트랙 소개(API-CRS-01), 코스 목록(API-CRS-02),
 *        코스 상세(API-CRS-03).
 * 책임 : - 공개 코스(is_public = TRUE)만 보여준다. 비회원도 볼 수 있다 (FR-CRS-001, 002, 004).
 *        - 진단을 마친 회원(COMPLETED)에게만 추천 트랙과 그 코스를 위에 보여준다. 건너뛴 회원·비회원은 없다.
 *        - 수업 수·공개 수업 목록·수강 상태는 lesson 영역의 계약 ⑪ 로 받는다 (공개 수업만).
 *        - P2(#189): 카테고리·검색(category, q). 수업 제목 검색은 lesson 의 LessonReader 로 받는다 (공개 수업만).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseBrowseService {

    private final TrackRepository trackRepository;
    private final CourseRepository courseRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final CourseLessonInfo courseLessonInfo;
    private final LessonReader lessonReader;

    /* U-09 트랙 소개. 없는 트랙은 TRACK_NOT_FOUND (E-01) */
    public TrackIntroView getTrackIntro(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.TRACK_NOT_FOUND));
        List<Course> courses = courseRepository.findByTrackIdAndIsPublicTrueOrderByCourseIdAsc(trackId);
        List<CourseCard> cards = toCards(courses, Map.of(track.getTrackId(), track.getName()));
        return new TrackIntroView(track.getTrackId(), track.getName(), track.getSortOrder(),
                track.getTarget(), track.getDescription(), cards);
    }

    /*
     * U-10 코스 목록. memberId 는 비회원이면 null, trackId 는 필터가 없으면 null.
     * 없는 트랙으로 거르면 오류 없이 빈 목록이다 (API-CRS-02 "결과 없음은 빈 목록").
     */
    public CourseListView getCourseList(Long memberId, Long trackId) {
        return getCourseList(memberId, trackId, null, null);
    }

    /*
     * U-10 코스 목록 — 카테고리·검색 (API-CRS-02 P2, FR-CRS-003·009, #189). 세 조건은 모두 만족하는 코스만.
     * - q: 앞뒤 공백을 지우고 빈 값이면 검색하지 않는다. 코스 제목이나 그 코스의 공개 수업 제목에 들어 있으면(부분 일치) 보여주고,
     *   수업 제목으로 걸린 카드에는 걸린 수업 제목을 붙인다. 대소문자는 구분하지 않는다 (DB 정렬 규칙 utf8mb4_0900_ai_ci 와 맞춤).
     * - 추천 블록: q 가 있으면 숨긴다. 트랙·카테고리가 있으면 추천 코스에도 같은 조건을 걸고, 남는 코스가 없으면 숨긴다.
     * - 없는 카테고리·결과 없음은 빈 목록 (명세 "이 명세에서 정한 것" 1~4)
     */
    public CourseListView getCourseList(Long memberId, Long trackId, Long categoryId, String q) {
        String keyword = q == null || q.isBlank() ? null : q.strip();
        List<Track> tracks = trackRepository.findAllByOrderBySortOrderAsc();
        Map<Long, String> trackNames = tracks.stream().collect(Collectors.toMap(Track::getTrackId, Track::getName));

        Map<Long, List<String>> lessonHits = keyword == null ? Map.of() : lessonReader.searchPublicLessonTitles(keyword);
        List<Course> courses = (trackId == null
                ? courseRepository.findByIsPublicTrueOrderByCourseIdAsc()
                : courseRepository.findByTrackIdAndIsPublicTrueOrderByCourseIdAsc(trackId)).stream()
                .filter(course -> categoryId == null || course.getCourseCategoryId().equals(categoryId))
                .filter(course -> keyword == null || contains(course.getTitle(), keyword) || lessonHits.containsKey(course.getCourseId()))
                .toList();
        Map<Long, List<CourseCard>> cardsByTrack = withMatchedLessons(toCards(courses, trackNames), lessonHits).stream()
                .collect(Collectors.groupingBy(CourseCard::trackId));

        List<TrackCourseGroup> groups = tracks.stream()
                .filter(track -> cardsByTrack.containsKey(track.getTrackId()))
                .map(track -> new TrackCourseGroup(track.getTrackId(), track.getName(), cardsByTrack.get(track.getTrackId())))
                .toList();
        int totalCount = groups.stream().mapToInt(group -> group.courses().size()).sum();

        List<TrackChip> chips = tracks.stream().map(track -> new TrackChip(track.getTrackId(), track.getName())).toList();
        List<CategoryChip> categories = courseCategoryRepository.findAllByOrderBySortOrderAscCourseCategoryIdAsc().stream()
                .map(category -> new CategoryChip(category.getCourseCategoryId(), category.getName()))
                .toList();
        TrackCourseGroup recommended = keyword != null ? null
                : filtered(recommendedFor(memberId, trackNames), trackId, categoryId, courses);
        return new CourseListView(chips, trackId, recommended, groups, totalCount, categories, categoryId, keyword);
    }

    /* 트랙·카테고리가 걸려 있으면 추천 코스도 같은 조건(courses: 이미 걸러진 공개 코스)만 남기고, 남는 게 없으면 숨긴다 */
    private TrackCourseGroup filtered(TrackCourseGroup recommended, Long trackId, Long categoryId, List<Course> courses) {
        if (recommended == null || (trackId == null && categoryId == null)) {
            return recommended;
        }
        Set<Long> allowed = courses.stream().map(Course::getCourseId).collect(Collectors.toSet());
        List<CourseCard> cards = recommended.courses().stream().filter(card -> allowed.contains(card.courseId())).toList();
        return cards.isEmpty() ? null : new TrackCourseGroup(recommended.trackId(), recommended.trackName(), cards);
    }

    /* 수업 제목으로 걸린 카드에 걸린 수업 제목을 붙인다 */
    private List<CourseCard> withMatchedLessons(List<CourseCard> cards, Map<Long, List<String>> lessonHits) {
        if (lessonHits.isEmpty()) {
            return cards;
        }
        return cards.stream()
                .map(card -> new CourseCard(card.courseId(), card.trackId(), card.title(), card.summary(), card.trackName(),
                        card.categoryName(), card.planDays(), card.lessonCount(), lessonHits.getOrDefault(card.courseId(), List.of())))
                .toList();
    }

    private static boolean contains(String text, String keyword) {
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /*
     * U-11 코스 상세 (API-CRS-03). memberId 는 비회원이면 null → 수강 상태 NONE.
     * 없거나 비공개인 코스는 COURSE_NOT_FOUND (E-01). 비공개 코스를 있다고 알리지 않는다.
     */
    public CourseDetailView getCourseDetail(Long memberId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .filter(Course::isPublic)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
        String trackName = trackRepository.findById(course.getTrackId()).map(Track::getName).orElse(null);
        String categoryName = courseCategoryRepository.findById(course.getCourseCategoryId())
                .map(CourseCategory::getName).orElse(null);
        String enrollmentStatus = memberId == null ? "NONE" : courseLessonInfo.getEnrollmentStatus(memberId, courseId);

        return new CourseDetailView(course.getCourseId(), course.getTitle(), course.getTrackId(), trackName, categoryName,
                course.getSummary(), course.getTarget(), course.getGoal(), course.getPlanDays(), course.getDescription(),
                courseLessonInfo.getPublicLessons(courseId), enrollmentStatus);
    }

    /* 진단을 마친 회원의 가장 최근 추천 트랙과 그 공개 코스. 비회원·건너뜀·결과 없음은 null */
    private TrackCourseGroup recommendedFor(Long memberId, Map<Long, String> trackNames) {
        if (memberId == null) {
            return null;
        }
        return diagnosisResultRepository.findFirstByMemberIdOrderByDiagnosisResultIdDesc(memberId)
                .filter(result -> result.getStatus() == DiagnosisStatus.COMPLETED)
                .map(result -> {
                    Long recommendedTrackId = result.getRecommendedTrackId();
                    List<Course> courses = courseRepository.findByTrackIdAndIsPublicTrueOrderByCourseIdAsc(recommendedTrackId);
                    return new TrackCourseGroup(recommendedTrackId, trackNames.get(recommendedTrackId), toCards(courses, trackNames));
                })
                .orElse(null);
    }

    private List<CourseCard> toCards(List<Course> courses, Map<Long, String> trackNames) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getCourseId).toList();
        Map<Long, Integer> lessonCounts = courseLessonInfo.countPublicLessons(courseIds);
        Map<Long, String> categoryNames = categoryNamesOf(courses.stream().map(Course::getCourseCategoryId).distinct().toList());

        return courses.stream()
                .map(course -> new CourseCard(course.getCourseId(), course.getTrackId(), course.getTitle(), course.getSummary(),
                        trackNames.get(course.getTrackId()), categoryNames.get(course.getCourseCategoryId()),
                        course.getPlanDays(), lessonCounts.getOrDefault(course.getCourseId(), 0)))
                .toList();
    }

    private Map<Long, String> categoryNamesOf(Collection<Long> categoryIds) {
        return courseCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CourseCategory::getCourseCategoryId, CourseCategory::getName));
    }
}
