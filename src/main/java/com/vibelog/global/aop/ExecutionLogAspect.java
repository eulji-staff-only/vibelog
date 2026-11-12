package com.vibelog.global.aop;

import com.vibelog.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/*
 * [ExecutionLogAspect]
 * 역할 : 채점·리포트·포트폴리오 제출 Service 메서드의 실행 시간과 예상 못한 예외를 로그로 남긴다 (NFR-PERF-002, 이슈 #433).
 *        성능을 높이는 장치가 아니라, 운영 중 느려지거나 실패한 곳을 로그만 보고 빨리 찾기 위한 관찰용이다.
 * 책임 : - 대상은 아래 4개 메서드뿐이다 (퀴즈 제출, 모의고사 제출, 오답 분석 리포트 조회, 포트폴리오 제출). 다른 메서드는 건드리지 않는다.
 *        - 정상으로 끝나면 INFO "[완료] 메서드 걸린시간" 한 줄을 남긴다 (주요 업무 흐름은 info, 코드 점검표 C-2, 이슈 #485).
 *          요청 ID 는 RequestLogFilter 가 MDC 에 넣어 두므로 같은 요청의 다른 로그와 묶어 볼 수 있다.
 *        - 걸린 시간이 기준(vibelog.aop.slow-threshold-ms, 기본 1500ms) 이상이면 [완료] 대신 WARN 한 줄을 남긴다 (한 요청에 한 줄).
 *        - 예상 못한 예외가 나면 어느 메서드에서 났는지 WARN 한 줄을 남기고 예외는 그대로 다시 던진다.
 *          스택트레이스와 오류 번호는 GlobalExceptionHandler 가 이미 남기므로 여기서 또 찍지 않는다.
 *        - BusinessException(규칙 위반)은 정상 흐름이라 예외 로그를 남기지 않는다. 처리가 끝난 것이 아니므로 [완료]도 남기지 않는다.
 *        - 인자·반환값·예외 메시지는 읽지도 기록하지도 않는다 (비밀번호·API 키·회원 정보가 섞일 수 있다).
 *        - 결과와 예외를 바꾸지 않는다. 시간만 재고 그대로 통과시킨다.
 *        - 가장 바깥(HIGHEST_PRECEDENCE)에서 감싸, 트랜잭션 커밋까지 포함한 시간(사용자가 기다린 시간)을 잰다.
 *        - 대상 클래스는 문자열로 지정해 다른 영역 클래스를 import 하지 않는다. 메서드 이름이 바뀌면 AOP 가 조용히 빠지므로
 *          ExecutionLogAspectTest 가 실제 메서드와 맞는지 확인한다.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExecutionLogAspect {

    static final String TARGETS =
            "execution(* com.vibelog.domain.quiz.model.service.QuizSolveService.submit(..))"
                    + " || execution(* com.vibelog.domain.exam.model.service.ExamService.submit(..))"
                    + " || execution(* com.vibelog.domain.exam.model.service.ExamReportService.getReport(..))"
                    + " || execution(* com.vibelog.domain.portfolio.model.service.PortfolioSubmitService.submit(..))";

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final long slowThresholdMillis;

    public ExecutionLogAspect(@Value("${vibelog.aop.slow-threshold-ms:1500}") long slowThresholdMillis) {
        this.slowThresholdMillis = slowThresholdMillis;
    }

    @Around(TARGETS)
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        boolean completed = false;
        try {
            Object result = joinPoint.proceed();
            completed = true;
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Throwable t) {
            log.warn("[예외] {} 에서 {} 발생", method, t.getClass().getSimpleName());
            throw t;
        } finally {
            long elapsedMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLI;
            if (elapsedMillis >= slowThresholdMillis) {
                log.warn("[느린 처리] {} {}ms (기준 {}ms)", method, elapsedMillis, slowThresholdMillis);
            } else if (completed) {
                log.info("[완료] {} {}ms", method, elapsedMillis);
            }
        }
    }
}
