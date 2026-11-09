package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.CourseCategoryRepository;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.DiagnosisResultRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.CourseCard;
import com.vibelog.domain.course.model.dto.CourseListView;
import com.vibelog.domain.course.model.dto.TrackChip;
import com.vibelog.domain.course.model.dto.TrackCourseGroup;
import com.vibelog.domain.course.model.dto.TrackIntroView;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.course.model.entity.CourseCategory;
import com.vibelog.domain.course.model.entity.DiagnosisStatus;
import com.vibelog.domain.course.model.entity.Track;
import com.vibelog.domain.lesson.model.policy.CourseLessonInfo;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * [CourseBrowseService]
 * 역할 : 학습자가 트랙·코스를 둘러보는 화면의 데이터를 만든다. 트랙 소개(API-CRS-01), 코스 목록(API-CRS-02).
 * 책임 : - 공개 코스(is_public = TRUE)만 보여준다. 비회원도 볼 수 있다 (FR-CRS-001, 004).
 *        - 진단을 마친 회원(COMPLETED)에게만 추천 트랙과 그 코스를 위에 보여준다. 건너뛴 회원·비회원은 없다.
 *        - 수업 수는 lesson 영역의 계약 ⑪ countPublicLessons 로 받는다 (코스 ID 묶음, 공개 수업만).
 *        - 카테고리·검색(category, q)은 P2라 받지 않는다.
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
        List<Track> tracks = trackRepository.findAllByOrderBySortOrderAsc();
        Map<Long, String> trackNames = tracks.stream().collect(Collectors.toMap(Track::getTrackId, Track::getName));

        List<Course> courses = trackId == null
                ? courseRepository.findByIsPublicTrueOrderByCourseIdAsc()
                : courseRepository.findByTrackIdAndIsPublicTrueOrderByCourseIdAsc(trackId);
        Map<Long, List<CourseCard>> cardsByTrack = toCards(courses, trackNames).stream()
                .collect(Collectors.groupingBy(CourseCard::trackId));

        List<TrackCourseGroup> groups = tracks.stream()
                .filter(track -> cardsByTrack.containsKey(track.getTrackId()))
                .map(track -> new TrackCourseGroup(track.getTrackId(), track.getName(), cardsByTrack.get(track.getTrackId())))
                .toList();
        int totalCount = groups.stream().mapToInt(group -> group.courses().size()).sum();

        List<TrackChip> chips = tracks.stream().map(track -> new TrackChip(track.getTrackId(), track.getName())).toList();
        return new CourseListView(chips, trackId, recommendedFor(memberId, trackNames), groups, totalCount);
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
