package com.vibelog.global.aop;

import com.vibelog.domain.exam.model.service.ExamReportService;
import com.vibelog.domain.exam.model.service.ExamService;
import com.vibelog.domain.portfolio.model.service.PortfolioSubmitService;
import com.vibelog.domain.quiz.model.service.QuizSolveService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 실행 시간·예외 기록 AOP 가 진짜 Spring 빈에 걸려 있는지 확인한다 (이슈 #433).
 * ExecutionLogAspectTest 는 로직만 보므로, 빈 등록이나 의존성이 빠져 AOP 가 조용히 안 걸리는 경우는 여기서 잡는다.
 * 로컬 MySQL 이 켜져 있어야 통과한다 (앱 전체를 띄운다).
 */
@SpringBootTest
class ExecutionLogAspectApplyTest {

    @Autowired
    QuizSolveService quizSolveService;
    @Autowired
    ExamService examService;
    @Autowired
    ExamReportService examReportService;
    @Autowired
    PortfolioSubmitService portfolioSubmitService;

    @Test
    void 대상_Service_4개에_실행_기록_Aspect가_걸려_있다() {
        assertThat(hasExecutionLogAdvice(quizSolveService)).isTrue();
        assertThat(hasExecutionLogAdvice(examService)).isTrue();
        assertThat(hasExecutionLogAdvice(examReportService)).isTrue();
        assertThat(hasExecutionLogAdvice(portfolioSubmitService)).isTrue();
    }

    private boolean hasExecutionLogAdvice(Object bean) {
        return bean instanceof Advised advised
                && Arrays.stream(advised.getAdvisors()).anyMatch(a -> a.getAdvice().toString().contains("ExecutionLogAspect"));
    }
}
