package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.lesson.model.dto.LessonImpactResult;
import com.vibelog.domain.lesson.model.service.LessonAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/*
 * [LessonImpactApiController]
 * 역할 : 수업 비공개 영향 확인 (API-ADM-32, FR-LSN-014, #118). 화면이 바뀌지 않으므로 JSON으로 답한다 (D-08).
 * 책임 : 운영자만 들어온다 (SecurityConfig 가 /api/admin/** 을 막는다). A-06의 비공개 경고 모달이 열기 전에 부른다.
 */
@RestController
@RequiredArgsConstructor
public class LessonImpactApiController {

    private final LessonAdminService lessonAdminService;

    @GetMapping("/api/admin/lessons/{lessonId}/impact")
    public LessonImpactResult impact(@PathVariable Long lessonId) {
        return new LessonImpactResult(lessonAdminService.getActiveLearnerCount(lessonId));
    }
}
