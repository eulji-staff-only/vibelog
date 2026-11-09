package com.vibelog.domain.course.controller;

import com.vibelog.domain.course.model.dto.CategoryOrderRequest;
import com.vibelog.domain.course.model.service.CourseCatalogAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * [CourseCatalogAdminApiController]
 * 역할 : 카테고리 드래그 순서 저장 (API-ADM-10, #95). 화면이 바뀌지 않아 JSON 으로 답한다.
 * 책임 : 운영자만 부른다 (SecurityConfig /api/admin/**). 실패는 GlobalExceptionHandler 가 {code, message} 로 보낸다.
 */
@RestController
@RequiredArgsConstructor
public class CourseCatalogAdminApiController {

    private final CourseCatalogAdminService courseCatalogAdminService;

    @PostMapping("/api/admin/categories/order")
    public ResponseEntity<Void> reorder(@Valid @RequestBody CategoryOrderRequest request) {
        courseCatalogAdminService.reorderCategories(request.courseCategoryIds());
        return ResponseEntity.noContent().build();
    }
}
