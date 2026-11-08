package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.dao.CourseRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.entity.Course;
import com.vibelog.domain.course.model.entity.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
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

    /* 코스 ID → 코스 이름. 없는 ID 는 결과에 빠진다. 운영자 회원 상세의 수강 코스(auth #119)가 쓴다 */
    public Map<Long, String> getCourseTitles(Collection<Long> courseIds) {
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Course::getTitle));
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
}
