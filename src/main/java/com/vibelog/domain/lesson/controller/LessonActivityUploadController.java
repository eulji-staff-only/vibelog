package com.vibelog.domain.lesson.controller;

import com.vibelog.domain.lesson.model.dto.ActivityImageResult;
import com.vibelog.domain.lesson.model.service.LessonActivityAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/*
 * [LessonActivityUploadController]
 * 역할 : 개념 본문 편집기에서 쓰는 이미지 올리기 (API-ADM-25, FR-LSN-011, #101). 화면이 바뀌지 않으므로
 *        JSON 으로 답한다 (D-08).
 * 책임 : 운영자만 들어온다 (SecurityConfig 가 /api/admin/** 을 막는다). 실패(PL-DAT-004 위반)는
 *        GlobalExceptionHandler 가 {code: INVALID_FILE, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class LessonActivityUploadController {

    private final LessonActivityAdminService lessonActivityAdminService;

    @PostMapping("/api/admin/uploads/images")
    public ActivityImageResult upload(@RequestParam("image") MultipartFile image) {
        return new ActivityImageResult(lessonActivityAdminService.uploadConceptImage(image));
    }
}
