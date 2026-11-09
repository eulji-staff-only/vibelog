package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.dao.DiagnosisOptionRepository;
import com.vibelog.domain.course.model.dao.DiagnosisQuestionRepository;
import com.vibelog.domain.course.model.dao.DiagnosisResultRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.DiagnosisResultView;
import com.vibelog.domain.course.model.dto.DiagnosisSurveyView;
import com.vibelog.domain.course.model.entity.DiagnosisOption;
import com.vibelog.domain.course.model.entity.DiagnosisQuestion;
import com.vibelog.domain.course.model.entity.DiagnosisResult;
import com.vibelog.domain.course.model.policy.TrackRecommendPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [DiagnosisService]
 * 역할 : 진단 설문·제출·건너뛰기·결과 (API-DGN-01~04, FR-DGN-001~003 / 진단 다시 하기 FR-DGN-005, #188).
 * 책임 : - 응답(고른 보기)은 저장하지 않고 진단 결과(완료·건너뜀, 추천 트랙)만 저장한다 (PL-DGN-001).
 *          추천 이유로 쓸 보기 내용은 제출 결과로만 돌려주고, Controller 가 flash 로 결과 화면에 넘긴다.
 *        - 다시 하기(P2): 제출은 지난 결과를 고치지 않고 새 행을 쌓는다(최근 행으로 추천). 진단 기록이 있는 회원의
 *          건너뛰기는 아무것도 저장하지 않는다(지난 추천을 SKIPPED 로 덮지 않게). 수강(enrollment)은 건드리지 않는다.
 *        - 답이 사용 중 문항과 맞지 않으면(문항이 바뀐 사이 제출 등) 저장하지 않고 빈 값을 돌려준다.
 *          화면은 오류 화면이 아니라 "설문 + 토스트"(③)라 예외로 던지지 않는다 (API-DGN-02 P2).
 *        - 추천 트랙 계산은 TrackRecommendPolicy 가 한다 (PL-DGN-002).
 *        - 결과 화면의 추천 트랙·코스는 CourseBrowseService 의 트랙 소개(API-CRS-01) 데이터를 그대로 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private final DiagnosisQuestionRepository diagnosisQuestionRepository;
    private final DiagnosisOptionRepository diagnosisOptionRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final TrackRepository trackRepository;
    private final TrackRecommendPolicy trackRecommendPolicy;
    private final CourseBrowseService courseBrowseService;

    /* 제출 결과: 저장한 결과 ID·추천 트랙과, 추천 이유로 보여 줄 보기 내용 (저장하지 않음) */
    public record SubmitResult(Long diagnosisResultId, Long recommendedTrackId, List<String> reasons) {
    }

    /* 진단 기록(완료·건너뜀)이 있는가. 설문 화면이 "다시 하기" 안내를 고를 때 쓴다 */
    public boolean hasRecord(Long memberId) {
        return diagnosisResultRepository.existsByMemberId(memberId);
    }

    /* U-07 설문: 사용 중인 문항을 순서대로, 문항마다 보기를 순서대로 */
    public DiagnosisSurveyView getSurvey() {
        List<DiagnosisQuestion> questions = diagnosisQuestionRepository.findByIsActiveTrueOrderBySortOrderAsc();
        Map<Long, List<DiagnosisOption>> optionsByQuestion = optionsOf(questions);

        return new DiagnosisSurveyView(questions.stream()
                .map(question -> new DiagnosisSurveyView.Question(question.getDiagnosisQuestionId(), question.getContent(),
                        optionsByQuestion.getOrDefault(question.getDiagnosisQuestionId(), List.of()).stream()
                                .map(option -> new DiagnosisSurveyView.Option(option.getDiagnosisOptionId(), option.getContent()))
                                .toList()))
                .toList());
    }

    /*
     * API-DGN-02 제출. 사용 중인 문항마다 보기를 정확히 하나씩 골라야 한다. 아니면 저장하지 않고 빈 값 (INVALID_INPUT,
     * 화면은 설문 + 토스트). 문항 수와 답 수가 다름, 같은 문항에 두 개, 사용 중 문항에 없는 보기(설문을 연 사이 바뀜), 빈 값.
     * 다시 하기여도 지난 결과는 고치지 않고 새 행을 저장한다 (API-DGN-02 P2).
     */
    @Transactional
    public Optional<SubmitResult> submit(Long memberId, List<Long> diagnosisOptionIds) {
        List<DiagnosisQuestion> questions = diagnosisQuestionRepository.findByIsActiveTrueOrderBySortOrderAsc();
        Optional<List<DiagnosisOption>> chosen = validChosenOptions(questions, diagnosisOptionIds);
        if (chosen.isEmpty()) {
            return Optional.empty();
        }

        Long recommendedTrackId = trackRecommendPolicy.recommend(chosen.get(), trackRepository.findAll());
        if (recommendedTrackId == null) {
            return Optional.empty();
        }
        DiagnosisResult saved = diagnosisResultRepository.save(DiagnosisResult.createCompleted(memberId, recommendedTrackId));

        List<String> reasons = chosen.get().stream()
                .filter(option -> option.getTrackId().equals(recommendedTrackId))
                .map(DiagnosisOption::getContent)
                .toList();
        return Optional.of(new SubmitResult(saved.getDiagnosisResultId(), recommendedTrackId, reasons));
    }

    /*
     * API-DGN-03 건너뛰기: 건너뜀으로 저장한다 (FR-DGN-003).
     * 진단 기록이 이미 있으면(다시 하기 중) 아무것도 저장하지 않는다 — 지난 추천을 SKIPPED 로 덮지 않게 (API-DGN-03 P2)
     */
    @Transactional
    public void skip(Long memberId) {
        if (diagnosisResultRepository.existsByMemberId(memberId)) {
            return;
        }
        diagnosisResultRepository.save(DiagnosisResult.createSkipped(memberId));
    }

    /*
     * API-DGN-04 결과. 가장 최근 결과가 없거나 건너뜀이면 빈 값 → Controller 가 /diagnosis 로 보낸다.
     * reasons·resultId 는 제출 직후 flash 로 받은 것. 이유는 resultId 가 가장 최근 결과와 같을 때만 쓴다
     * (다른 기기에서 연달아 제출해 최근 결과가 바뀌면 이유 없이 트랙 설명만, API-DGN-02 P2).
     */
    public Optional<DiagnosisResultView> getResult(Long memberId, List<String> reasons, Long resultId) {
        return diagnosisResultRepository.findFirstByMemberIdOrderByDiagnosisResultIdDesc(memberId)
                .filter(DiagnosisResult::isCompleted)
                .map(result -> {
                    boolean sameResult = resultId != null && resultId.equals(result.getDiagnosisResultId());
                    Long recommendedTrackId = result.getRecommendedTrackId();
                    List<DiagnosisResultView.OtherTrack> others = trackRepository.findAllByOrderBySortOrderAsc().stream()
                            .filter(track -> !track.getTrackId().equals(recommendedTrackId))
                            .map(track -> new DiagnosisResultView.OtherTrack(track.getTrackId(), track.getName(),
                                    track.getTarget(), track.getDescription()))
                            .toList();
                    return new DiagnosisResultView(courseBrowseService.getTrackIntro(recommendedTrackId),
                            sameResult && reasons != null ? reasons : List.of(), others);
                });
    }

    /* 문항마다 하나씩 고른 보기. 맞지 않으면 빈 값 */
    private Optional<List<DiagnosisOption>> validChosenOptions(List<DiagnosisQuestion> questions, List<Long> diagnosisOptionIds) {
        if (questions.isEmpty() || diagnosisOptionIds == null || diagnosisOptionIds.size() != questions.size()
                || diagnosisOptionIds.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        Map<Long, DiagnosisOption> optionsById = optionsOf(questions).values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(DiagnosisOption::getDiagnosisOptionId, option -> option));

        Set<Long> answeredQuestions = new HashSet<>();
        List<DiagnosisOption> chosen = diagnosisOptionIds.stream().map(optionsById::get).toList();
        for (DiagnosisOption option : chosen) {
            // 사용 중인 문항의 보기가 아니거나, 같은 문항에 두 번 답함
            if (option == null || !answeredQuestions.add(option.getDiagnosisQuestionId())) {
                return Optional.empty();
            }
        }
        return Optional.of(chosen);
    }

    private Map<Long, List<DiagnosisOption>> optionsOf(List<DiagnosisQuestion> questions) {
        List<Long> questionIds = questions.stream().map(DiagnosisQuestion::getDiagnosisQuestionId).toList();
        return diagnosisOptionRepository.findByDiagnosisQuestionIdInOrderBySortOrderAsc(questionIds).stream()
                .collect(Collectors.groupingBy(DiagnosisOption::getDiagnosisQuestionId));
    }
}
