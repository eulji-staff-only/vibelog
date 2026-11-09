package com.vibelog.domain.course.model.policy;

import com.vibelog.domain.course.model.entity.DiagnosisOption;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [DiagnosisAnswerPolicy]
 * 역할 : 진단 답이 맞는 모양인지 본다 — 사용 중인 문항마다 보기를 정확히 하나씩 골랐는가 (FR-DGN-001·002, PL-DGN-002).
 * 책임 : - 진단 제출(DiagnosisService, API-DGN-02)과 운영자 테스트 응답(DiagnosisAdminService, API-ADM-45)이 같이 쓴다.
 *          명세가 "테스트 응답은 실제 진단과 같은 계산"을 약속하므로 검사 규칙을 한 곳에만 둔다 (#333).
 *        - 맞지 않는 경우: 사용 중 문항이 없음, 답 수가 문항 수와 다름, 빈 값, 사용 중 문항의 보기가 아님(문항이 바뀐 사이 제출 등),
 *          같은 문항에 두 개. 그러면 빈 값을 돌려준다 (예외가 아님 — 두 화면 모두 "같은 화면 + 토스트"로 안내한다).
 *        - DB 를 보지 않는다. 사용 중 문항 수와 그 문항들의 보기는 부르는 쪽이 모아서 넘긴다 (TrackRecommendPolicy 와 같은 방식).
 */
@Component
public class DiagnosisAnswerPolicy {

    /*
     * activeQuestionCount: 사용 중인 문항 수, activeOptions: 사용 중인 문항들의 보기 전부, chosenOptionIds: 회원이 고른 보기 ID.
     * 맞으면 고른 순서대로 보기, 아니면 빈 값
     */
    public Optional<List<DiagnosisOption>> chosenOptions(int activeQuestionCount, List<DiagnosisOption> activeOptions,
                                                         List<Long> chosenOptionIds) {
        if (activeQuestionCount == 0 || chosenOptionIds == null || chosenOptionIds.size() != activeQuestionCount
                || chosenOptionIds.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        Map<Long, DiagnosisOption> optionsById = activeOptions.stream()
                .collect(Collectors.toMap(DiagnosisOption::getDiagnosisOptionId, Function.identity(), (a, b) -> a));

        Set<Long> answeredQuestions = new HashSet<>();
        List<DiagnosisOption> chosen = chosenOptionIds.stream().map(optionsById::get).toList();
        for (DiagnosisOption option : chosen) {
            // 사용 중인 문항의 보기가 아니거나, 같은 문항에 두 번 답함
            if (option == null || !answeredQuestions.add(option.getDiagnosisQuestionId())) {
                return Optional.empty();
            }
        }
        return Optional.of(chosen);
    }
}
