package com.vibelog.global.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibelog.domain.exam.model.service.ExamReportService;
import com.vibelog.domain.exam.model.service.ExamService;
import com.vibelog.domain.portfolio.model.service.PortfolioSubmitService;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * 실행 시간·예외 기록 AOP (이슈 #433): 결과·예외를 바꾸지 않고, 느릴 때와 예상 못한 예외일 때만 로그를 남기며,
 * 인자·예외 메시지는 기록하지 않는다. 대상 4개 메서드가 실제 코드와 맞는지도 확인한다 (이름이 바뀌면 AOP 가 조용히 빠진다).
 * Spring·DB 없이 돌아간다.
 */
class ExecutionLogAspectTest {

    private static final long HOUR_MILLIS = 3_600_000L;

    private final Logger aspectLogger = (Logger) LoggerFactory.getLogger(ExecutionLogAspect.class);
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void attachAppender() {
        logs = new ListAppender<>();
        logs.start();
        aspectLogger.addAppender(logs);
    }

    @AfterEach
    void detachAppender() {
        aspectLogger.detachAppender(logs);
    }

    @Test
    void 결과를_바꾸지_않고_그대로_돌려준다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("QuizSolveService", "submit");
        doReturn("채점 결과").when(joinPoint).proceed();

        Object result = new ExecutionLogAspect(HOUR_MILLIS).logExecution(joinPoint);

        assertThat(result).isEqualTo("채점 결과");
    }

    @Test
    void 기준보다_빠르면_로그를_남기지_않는다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("QuizSolveService", "submit");

        new ExecutionLogAspect(HOUR_MILLIS).logExecution(joinPoint);

        assertThat(logs.list).isEmpty();
    }

    @Test
    void 기준_이상으로_걸리면_메서드_이름과_시간을_경고로_남긴다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("ExamService", "submit");

        new ExecutionLogAspect(0).logExecution(joinPoint);

        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logs.list.get(0).getFormattedMessage()).contains("[느린 처리]", "ExamService.submit", "기준 0ms");
    }

    @Test
    void 규칙_위반_예외는_그대로_던지고_예외_로그는_남기지_않는다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("PortfolioSubmitService", "submit");
        BusinessException violation = new BusinessException(CommonErrorCode.NOT_OWNER);
        when(joinPoint.proceed()).thenThrow(violation);

        assertThatThrownBy(() -> new ExecutionLogAspect(HOUR_MILLIS).logExecution(joinPoint)).isSameAs(violation);

        assertThat(logs.list).isEmpty();
    }

    @Test
    void 예상_못한_예외는_그대로_다시_던지고_어느_메서드인지만_남긴다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("ExamReportService", "getReport");
        IllegalStateException unexpected = new IllegalStateException("password=sk-secret-1234");
        when(joinPoint.proceed()).thenThrow(unexpected);

        assertThatThrownBy(() -> new ExecutionLogAspect(HOUR_MILLIS).logExecution(joinPoint)).isSameAs(unexpected);

        assertThat(logs.list).hasSize(1);
        String message = logs.list.get(0).getFormattedMessage();
        assertThat(message).contains("[예외]", "ExamReportService.getReport", "IllegalStateException");
        assertThat(message).doesNotContain("sk-secret-1234");   // 예외 메시지는 기록하지 않는다
        assertThat(logs.list.get(0).getThrowableProxy()).isNull();   // 스택트레이스는 GlobalExceptionHandler 가 남긴다
    }

    @Test
    void 인자_값은_읽지도_기록하지도_않는다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("QuizSolveService", "submit");

        new ExecutionLogAspect(0).logExecution(joinPoint);

        verify(joinPoint, never()).getArgs();
    }

    @Test
    void 대상_4개_메서드가_실제_코드와_맞는다() {
        assertThat(matchesEvery(QuizSolveService.class, "submit")).isTrue();
        assertThat(matchesEvery(ExamService.class, "submit")).isTrue();
        assertThat(matchesEvery(ExamReportService.class, "getReport")).isTrue();
        assertThat(matchesEvery(PortfolioSubmitService.class, "submit")).isTrue();
    }

    @Test
    void 대상이_아닌_메서드는_건드리지_않는다() {
        assertThat(matchesEvery(QuizSolveService.class, "getPage")).isFalse();
        assertThat(matchesEvery(ExamService.class, "saveAnswer")).isFalse();
        assertThat(matchesEvery(PortfolioSubmitService.class, "submitPersonal")).isFalse();
        assertThat(matchesEvery(PortfolioSubmitService.class, "edit")).isFalse();
    }

    private ProceedingJoinPoint joinPoint(String className, String methodName) {
        Signature signature = mock(Signature.class);
        doReturn(classNamed(className)).when(signature).getDeclaringType();
        when(signature.getName()).thenReturn(methodName);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    private Class<?> classNamed(String simpleName) {
        return List.of(QuizSolveService.class, ExamService.class, ExamReportService.class, PortfolioSubmitService.class).stream()
                .filter(c -> c.getSimpleName().equals(simpleName))
                .findFirst()
                .orElseThrow();
    }

    /* 그 이름의 메서드가 하나 이상 있고, 전부 대상으로 잡히는지 */
    private boolean matchesEvery(Class<?> type, String methodName) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(ExecutionLogAspect.TARGETS);
        List<Method> methods = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().equals(methodName)).toList();
        assertThat(methods).as("%s.%s 메서드가 있어야 한다", type.getSimpleName(), methodName).isNotEmpty();
        return methods.stream().allMatch(m -> pointcut.matches(m, type));
    }
}
