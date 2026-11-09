package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.dao.CourseCategoryRepository;
import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.DiagnosisResultRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.course.model.entity.CourseCategory;
import com.vibelog.domain.course.model.entity.DiagnosisResult;
import com.vibelog.domain.course.model.entity.DiagnosisStatus;
import com.vibelog.domain.course.model.entity.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [CourseReader]
 * 역할 : 다른 영역이 course 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 *        - course 기반(#83)보다 먼저 운영자 회원 상세(#119)가 만들었다. #83 은 이 클래스에 이어서 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReader {

    private final CourseRepository courseRepository;
    private final TrackRepository trackRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;

    /* 코스 ID → 코스 이름. 없는 ID 는 결과에 빠진다. 운영자 회원 상세의 수강 코스(auth #119)가 쓴다 */
    public Map<Long, String> getCourseTitles(Collection<Long> courseIds) {
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Course::getTitle));
    }

    /* 주어진 코스 중 공개 코스의 ID. 없는 ID·비공개 코스는 빠진다. 개념 태그 검색·연관 개념(lesson #192, API-LSN-07·09)이 쓴다 */
    public Set<Long> getPublicCourseIds(Collection<Long> courseIds) {
        return courseRepository.findAllById(courseIds).stream()
                .filter(Course::isPublic)
                .map(Course::getCourseId)
                .collect(Collectors.toSet());
    }

    /* 코스 존재 여부(공개 여부는 보지 않음). 운영자 수업 만들기(API-ADM-17, #100)가 COURSE_NOT_FOUND 판단에 쓴다 */
    public boolean existsById(Long courseId) {
        return courseRepository.existsById(courseId);
    }

    /* 코스 ID → 트랙 ID. 없는 ID 는 결과에 빠진다. 운영자 대시보드(admin #120, FR-ADM-002)가 코스를 트랙별로 묶을 때 쓴다 */
    public Map<Long, Long> getTrackIdsByCourse(Collection<Long> courseIds) {
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Course::getTrackId));
    }

    /* 트랙 ID → 트랙 이름. 없는 ID 는 결과에 빠진다. 운영자 대시보드(admin #120, FR-ADM-002)가 쓴다 */
    public Map<Long, String> getTrackTitles(Collection<Long> trackIds) {
        return trackRepository.findAllById(trackIds).stream()
                .collect(Collectors.toMap(Track::getTrackId, Track::getName));
    }

    /* 카테고리 ID → 카테고리 이름. 없는 ID 는 결과에 빠진다 */
    public Map<Long, String> getCategoryNames(Collection<Long> categoryIds) {
        return courseCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CourseCategory::getCourseCategoryId, CourseCategory::getName));
    }

    /*
     * 회원 ID → 추천받은 트랙 ID. 기간 내(diagnosis_result.created_at 기준) 완료(COMPLETED)한 진단만 보고,
     * 한 회원이 여러 번 했으면 가장 최근 한 건의 추천 트랙이다. from·to 가 null 이면 그 쪽 제한 없음.
     * 운영자 확장 대시보드(admin #194, FR-ADM-003)의 "추천 트랙 vs 실제 선택 트랙"이 쓴다
     */
    public Map<Long, Long> findDiagnosisRecommendedTracks(LocalDateTime from, LocalDateTime to) {
        Map<Long, Long> recommended = new LinkedHashMap<>();
        for (DiagnosisResult result : diagnosisResultRepository.findCompletedBetween(from, to)) {   // 오래된 것부터라 뒤의 것이 덮어쓴다
            recommended.put(result.getMemberId(), result.getRecommendedTrackId());
        }
        return recommended;
    }

    /*
     * 기간 내 진단 결과 수를 상태별로 센다 (diagnosis_result.created_at 기준). 키는 COMPLETED·SKIPPED 둘 다 항상 있고 없으면 0.
     * from·to 가 null 이면 그 쪽 제한 없음. 운영자 확장 대시보드(admin #194, FR-ADM-003)가 쓴다
     */
    public Map<String, Integer> countDiagnosisResultsByStatus(LocalDateTime from, LocalDateTime to) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DiagnosisStatus status : DiagnosisStatus.values()) {
            counts.put(status.name(), 0);
        }
        for (Object[] row : diagnosisResultRepository.countByStatusBetween(from, to)) {
            counts.put(((DiagnosisStatus) row[0]).name(), ((Long) row[1]).intValue());
        }
        return counts;
    }
}
