package com.vibelog.domain.lesson.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [LessonReader]
 * 역할 : 다른 영역이 lesson 의 값을 읽기만 할 때 쓰는 창구 (08 문서 규칙 1).
 * 책임 : - 읽기 전용 메서드만 둔다. 목록용 메서드는 ID 묶음을 받는다 (예: Map<Long, X> getXxx(Collection<Long> ids)).
 *        - 필요한 메서드가 없으면 부르는 쪽이 추가한다. 기존 메서드의 이름·입력·출력은 바꾸지 않는다.
 *        - 지금은 부르는 쪽이 없어 메서드가 없다.
 */
@Service
@Transactional(readOnly = true)
public class LessonReader {
}
