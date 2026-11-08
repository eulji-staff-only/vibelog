package com.vibelog.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/*
 * [GlobalExceptionHandler]
 * 역할 : 모든 Controller 에서 발생한 예외를 한 곳에서 받아 에러 화면(error/error)으로 응답한다.
 * 책임 : - Controller / Service 에 try-catch 가 흩어지지 않게 한다. (Controller 는 정상 흐름만 작성)
 *        - 예상하지 못한 예외는 로그를 남기고, 내부 정보(스택트레이스)는 화면에 노출하지 않는다.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /* 도메인 / 비즈니스 규칙 위반 -> ErrorCode 의 상태코드와 메시지 */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e) {
        return errorPage(e.getErrorCode().getStatus(), e.getMessage());
    }

    /* @Valid DTO 검증 실패 -> 첫 번째 필드 에러 메시지 */
    @ExceptionHandler(BindException.class)
    public ModelAndView handleBindException(BindException e) {
        FieldError fieldError = e.getFieldError();
        if (fieldError == null) {
            return errorPage(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요.");
        }
        String message = fieldError.isBindingFailure()
                ? fieldError.getField() + "의 형식이 올바르지 않습니다."
                : fieldError.getDefaultMessage();
        return errorPage(HttpStatus.BAD_REQUEST, message);
    }

    /* @RequestParam 등에 직접 붙인 검증(@Min 등) 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ModelAndView handleMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력 값을 확인해주세요.");
        return errorPage(HttpStatus.BAD_REQUEST, message);
    }

    /* 필수 파라미터 누락 / 타입 불일치 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ModelAndView handleBadRequest(Exception e) {
        return errorPage(HttpStatus.BAD_REQUEST, "요청 값을 확인해주세요.");
    }

    /* 정책으로 걸러지지 않은 DB 제약조건 위반 (FK, UNIQUE 등) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DB 제약조건 위반", e);
        return errorPage(HttpStatus.CONFLICT, "요청을 처리할 수 없습니다. 입력 값을 확인해주세요.");
    }

    /* application.yaml 의 업로드 크기 제한 초과 -> 파일 규칙 안내 (#79) */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return errorPage(HttpStatus.BAD_REQUEST, CommonErrorCode.INVALID_FILE.getMessage());
    }

    /* 존재하지 않는 URL */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResource(NoResourceFoundException e) {
        return errorPage(HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다.");
    }

    /* 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        log.error("예상하지 못한 예외", e);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private ModelAndView errorPage(HttpStatus status, String message) {
        ModelAndView mv = new ModelAndView("error/error");
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("message", message);
        return mv;
    }
}
