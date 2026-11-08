package com.vibelog.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/*
 * [GlobalExceptionHandler]
 * 역할 : 모든 Controller 에서 발생한 예외를 한 곳에서 받아 응답한다.
 *        화면 요청은 에러 화면(error/error), /api/** 요청은 JSON {code, message} (D-08, API 명세 7-1 ⑤).
 * 책임 : - Controller / Service 에 try-catch 가 흩어지지 않게 한다. (Controller 는 정상 흐름만 작성)
 *        - 예상하지 못한 예외는 로그를 남기고, 내부 정보(스택트레이스)는 화면·JSON 에 노출하지 않는다.
 *        - 7장 표에 이름이 없는 실패는 SERVER_ERROR(500) 로 보낸다.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_INPUT = "INVALID_INPUT";

    /* 도메인 / 비즈니스 규칙 위반 -> ErrorCode 의 상태코드와 메시지 */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return error(request, errorCode.getStatus(), errorCode.name(), e.getMessage());
    }

    /* @Valid DTO 검증 실패 -> 첫 번째 필드 에러 메시지 (JSON 본문 검증 실패 MethodArgumentNotValidException 도 여기로 온다) */
    @ExceptionHandler(BindException.class)
    public ModelAndView handleBindException(BindException e, HttpServletRequest request) {
        FieldError fieldError = e.getFieldError();
        if (fieldError == null) {
            return error(request, HttpStatus.BAD_REQUEST, INVALID_INPUT, "입력 값을 확인해주세요.");
        }
        String message = fieldError.isBindingFailure()
                ? fieldError.getField() + "의 형식이 올바르지 않습니다."
                : fieldError.getDefaultMessage();
        return error(request, HttpStatus.BAD_REQUEST, INVALID_INPUT, message);
    }

    /* @RequestParam 등에 직접 붙인 검증(@Min 등) 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ModelAndView handleMethodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력 값을 확인해주세요.");
        return error(request, HttpStatus.BAD_REQUEST, INVALID_INPUT, message);
    }

    /* 필수 파라미터 누락 / 타입 불일치 / JSON 본문을 읽을 수 없음 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ModelAndView handleBadRequest(Exception e, HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, INVALID_INPUT, "요청 값을 확인해주세요.");
    }

    /*
     * 지원하지 않는 HTTP 메서드(405 METHOD_NOT_ALLOWED) / Content-Type(415 UNSUPPORTED_MEDIA_TYPE) / 응답 형식 Accept(406 NOT_ACCEPTABLE).
     * 요청을 잘못 보낸 경우라 서버 오류(500)로 바꾸지 않고 Spring 이 정한 상태와 헤더(Allow·Accept)를 그대로 보낸다
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    public ModelAndView handleUnsupported(Exception e, HttpServletRequest request, HttpServletResponse response) {
        org.springframework.web.ErrorResponse spring = (org.springframework.web.ErrorResponse) e;
        spring.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        HttpStatus status = HttpStatus.valueOf(spring.getStatusCode().value());
        return error(request, status, status.name(), "요청 방식을 확인해주세요.");
    }

    /* 정책으로 걸러지지 않은 DB 제약조건 위반 (FK, UNIQUE 등). INVALID_INPUT 은 400 이라 409 는 CONFLICT 로 따로 둔다 (D-08) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("DB 제약조건 위반", e);
        return error(request, HttpStatus.CONFLICT, "CONFLICT", "요청을 처리할 수 없습니다. 입력 값을 확인해주세요.");
    }

    /* application.yaml 의 업로드 크기 제한 초과 -> 파일 규칙 안내 (#79) */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, CommonErrorCode.INVALID_FILE.name(), CommonErrorCode.INVALID_FILE.getMessage());
    }

    /* 존재하지 않는 URL */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "NOT_FOUND", "요청하신 페이지를 찾을 수 없습니다.");
    }

    /* 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        log.error("예상하지 못한 예외", e);
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "잠시 후 다시 시도해 주세요.");
    }

    private ModelAndView error(HttpServletRequest request, HttpStatus status, String code, String message) {
        ModelAndView mv;
        if (ErrorResponse.isApiRequest(request)) {
            MappingJackson2JsonView json = new MappingJackson2JsonView();
            json.setExtractValueFromSingleKeyModel(true);   // {"error": {...}} 이 아니라 {...} 로 보낸다
            mv = new ModelAndView(json, "error", new ErrorResponse(code, message));
        } else {
            mv = new ModelAndView("error/error");
            mv.addObject("status", status.value());
            mv.addObject("message", message);
        }
        mv.setStatus(status);
        return mv;
    }
}
