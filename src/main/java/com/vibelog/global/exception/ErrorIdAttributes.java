package com.vibelog.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/*
 * [ErrorIdAttributes]
 * 역할 : Controller 를 거치지 않은 오류(화면 템플릿 오류, 필터 오류 등)가 "/error" 로 갈 때 쓰는 값에 문의용 오류 번호를 더한다 (PL-ERR-003, #365).
 * 책임 : 5xx 면 GlobalExceptionHandler 와 같은 모양의 오류 번호(errorId)를 만들어 로그에 남기고, error/500.html 이 화면에 보여 준다.
 *        Controller·Service 예외는 GlobalExceptionHandler 가 처리하므로 여기로 오지 않는다.
 */
@Slf4j
@Component
public class ErrorIdAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
        if (attributes.get("status") instanceof Integer status && status >= 500) {
            String errorId = GlobalExceptionHandler.newErrorId();
            log.error("처리하지 못한 오류 [오류 번호 {}] {}", errorId, attributes.get("path"), getError(webRequest));
            attributes.put("errorId", errorId);
        }
        return attributes;
    }
}
