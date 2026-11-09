package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.exception.CourseErrorCode;
import com.vibelog.domain.course.model.dao.DiagnosisOptionRepository;
import com.vibelog.domain.course.model.dao.DiagnosisQuestionRepository;
import com.vibelog.domain.course.model.dao.TrackRepository;
import com.vibelog.domain.course.model.dto.CourseOption;
import com.vibelog.domain.course.model.dto.DiagnosisAdminView;
import com.vibelog.domain.course.model.dto.DiagnosisOptionForm;
import com.vibelog.domain.course.model.dto.DiagnosisPreview;
import com.vibelog.domain.course.model.dto.DiagnosisQuestionForm;
import com.vibelog.domain.course.model.entity.DiagnosisOption;
import com.vibelog.domain.course.model.entity.DiagnosisQuestion;
import com.vibelog.domain.course.model.entity.Track;
import com.vibelog.domain.course.model.policy.TrackRecommendPolicy;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [DiagnosisAdminService]
 * 역할 : 운영자의 진단 설정 (A-09, API-ADM-41~45, FR-DGN-004, #187).
 * 책임 : - 문항은 지우지 않고 사용·중지만 한다. 마지막 사용 중 문항은 중지할 수 없다 (사용 중 문항이 없으면 진단을 못 한다).
 *        - 보기는 2개 이상, 점수 1 이상, 있는 트랙만 (어기면 INVALID_INPUT — 화면은 컨트롤러가 먼저 칸 메시지로 막는다).
 *        - 수정: 문항 내용·순서와 보기의 트랙·점수·순서는 그 자리에서 고친다. 보기 내용이 바뀐 보기는 지우고 새 보기(새 ID)로
 *          만든다 — 고치기 전 설문으로 제출한 답이 없는 보기가 되어 다시 진단하게 (admin.md API-ADM-43, course.md 결정 7).
 *          폼에 없는 기존 보기는 지운다. 응답을 저장하지 않아(PL-DGN-001) 보기를 지워도 끊기는 기록이 없다.
 *        - 테스트 응답: 실제 진단과 같은 계산(TrackRecommendPolicy)으로 추천 트랙을 구하기만 하고 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisAdminService {

    private final DiagnosisQuestionRepository diagnosisQuestionRepository;
    private final DiagnosisOptionRepository diagnosisOptionRepository;
    private final TrackRepository trackRepository;
    private final TrackRecommendPolicy trackRecommendPolicy;

    /* API-ADM-41 */
    public DiagnosisAdminView getView() {
        List<Track> tracks = trackRepository.findAllByOrderBySortOrderAsc();
        Map<Long, String> trackNames = tracks.stream().collect(Collectors.toMap(Track::getTrackId, Track::getName));
        List<DiagnosisQuestion> questions = diagnosisQuestionRepository.findAllByOrderBySortOrderAscDiagnosisQuestionIdAsc();
        Map<Long, List<DiagnosisOption>> optionsByQuestion = diagnosisOptionRepository.findByDiagnosisQuestionIdInOrderBySortOrderAsc(
                        questions.stream().map(DiagnosisQuestion::getDiagnosisQuestionId).toList()).stream()
                .collect(Collectors.groupingBy(DiagnosisOption::getDiagnosisQuestionId));

        return new DiagnosisAdminView(
                questions.stream()
                        .map(question -> new DiagnosisAdminView.Question(question.getDiagnosisQuestionId(), question.getContent(),
                                question.getSortOrder(), question.isActive(),
                                optionsByQuestion.getOrDefault(question.getDiagnosisQuestionId(), List.of()).stream()
                                        .map(option -> new DiagnosisAdminView.Option(option.getDiagnosisOptionId(), option.getContent(),
                                                option.getTrackId(), trackNames.get(option.getTrackId()), option.getScore()))
                                        .toList()))
                        .toList(),
                tracks.stream().map(track -> new CourseOption(track.getTrackId(), track.getName())).toList());
    }

    /* 수정 모달에 채울 값 (보기 칸은 빈 줄을 더해 넉넉히) */
    public DiagnosisQuestionForm getForm(Long diagnosisQuestionId) {
        DiagnosisQuestion question = getQuestion(diagnosisQuestionId);
        List<DiagnosisOptionForm> options = diagnosisOptionRepository.findByDiagnosisQuestionIdOrderBySortOrderAsc(diagnosisQuestionId).stream()
                .map(option -> DiagnosisOptionForm.of(option.getDiagnosisOptionId(), option.getContent(), option.getTrackId(), option.getScore()))
                .toList();
        return DiagnosisQuestionForm.of(question.getContent(), question.getSortOrder(), options).withBlankRows();
    }

    /* API-ADM-42. 사용 중으로 저장하고, 보기 순서는 폼에 적힌 순서대로 1부터 */
    @Transactional
    public void createQuestion(DiagnosisQuestionForm form) {
        requireValid(form);
        DiagnosisQuestion question = diagnosisQuestionRepository.save(DiagnosisQuestion.create(form.getContent(), form.getSortOrder()));
        List<DiagnosisOptionForm> options = form.filledOptions();
        for (int i = 0; i < options.size(); i++) {
            DiagnosisOptionForm option = options.get(i);
            diagnosisOptionRepository.save(DiagnosisOption.create(question.getDiagnosisQuestionId(), option.getTrackId(),
                    option.getContent(), option.getScore(), i + 1));
        }
    }

    /* API-ADM-43 */
    @Transactional
    public void updateQuestion(Long diagnosisQuestionId, DiagnosisQuestionForm form) {
        DiagnosisQuestion question = getQuestion(diagnosisQuestionId);
        requireValid(form);
        question.edit(form.getContent(), form.getSortOrder());

        Map<Long, DiagnosisOption> existing = diagnosisOptionRepository.findByDiagnosisQuestionIdOrderBySortOrderAsc(diagnosisQuestionId)
                .stream().collect(Collectors.toMap(DiagnosisOption::getDiagnosisOptionId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        List<DiagnosisOptionForm> options = form.filledOptions();
        for (int i = 0; i < options.size(); i++) {
            DiagnosisOptionForm option = options.get(i);
            DiagnosisOption saved = option.getDiagnosisOptionId() == null ? null : existing.get(option.getDiagnosisOptionId());
            if (saved != null && saved.getContent().equals(option.getContent()) && kept.add(saved.getDiagnosisOptionId())) {
                saved.change(option.getTrackId(), option.getScore(), i + 1);
            } else {
                // 새 보기이거나 내용이 바뀐 보기: 새 ID 로 만든다 (바뀐 예전 보기는 kept 에 없어 아래에서 지워진다)
                diagnosisOptionRepository.save(DiagnosisOption.create(diagnosisQuestionId, option.getTrackId(),
                        option.getContent(), option.getScore(), i + 1));
            }
        }
        diagnosisOptionRepository.deleteAll(existing.values().stream()
                .filter(option -> !kept.contains(option.getDiagnosisOptionId()))
                .toList());
    }

    /*
     * API-ADM-44. 바꿨으면 true. 마지막 사용 중 문항을 중지하려 하면 false (화면은 토스트, 오류 화면이 아니라 예외로 던지지 않는다)
     */
    @Transactional
    public boolean changeActive(Long diagnosisQuestionId, boolean active) {
        DiagnosisQuestion question = getQuestion(diagnosisQuestionId);
        if (!active && question.isActive() && diagnosisQuestionRepository.countByIsActiveTrue() <= 1) {
            return false;
        }
        question.changeActive(active);
        return true;
    }

    /*
     * API-ADM-45 테스트 응답. 사용 중인 문항마다 보기를 하나씩 골라야 한다 (아니면 빈 값 → 화면은 토스트).
     * 실제 진단과 같은 계산으로 추천 트랙과 트랙별 합계를 돌려주고, 아무것도 저장하지 않는다.
     */
    public Optional<DiagnosisPreview> preview(List<Long> diagnosisOptionIds) {
        List<DiagnosisQuestion> questions = diagnosisQuestionRepository.findByIsActiveTrueOrderBySortOrderAsc();
        if (questions.isEmpty() || diagnosisOptionIds == null || diagnosisOptionIds.size() != questions.size()
                || diagnosisOptionIds.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        Map<Long, DiagnosisOption> optionsById = diagnosisOptionRepository.findByDiagnosisQuestionIdInOrderBySortOrderAsc(
                        questions.stream().map(DiagnosisQuestion::getDiagnosisQuestionId).toList()).stream()
                .collect(Collectors.toMap(DiagnosisOption::getDiagnosisOptionId, Function.identity()));
        List<DiagnosisOption> chosen = diagnosisOptionIds.stream().map(optionsById::get).toList();
        Set<Long> answered = new HashSet<>();
        if (chosen.stream().anyMatch(option -> option == null || !answered.add(option.getDiagnosisQuestionId()))) {
            return Optional.empty();
        }

        List<Track> tracks = trackRepository.findAllByOrderBySortOrderAsc();
        Long recommendedTrackId = trackRecommendPolicy.recommend(chosen, tracks);
        Map<Long, Long> sums = trackRecommendPolicy.sumByTrack(chosen);
        String recommendedName = tracks.stream().filter(track -> track.getTrackId().equals(recommendedTrackId))
                .map(Track::getName).findFirst().orElse(null);
        return Optional.of(new DiagnosisPreview(recommendedTrackId, recommendedName, tracks.stream()
                .map(track -> new DiagnosisPreview.TrackScore(track.getTrackId(), track.getName(), sums.getOrDefault(track.getTrackId(), 0L)))
                .toList()));
    }

    /* 컨트롤러 검사를 거치지 않고 불러도 규칙이 깨지지 않게 다시 본다. 트랙이 있는지는 여기서만 본다 */
    private void requireValid(DiagnosisQuestionForm form) {
        if (form.getContent() == null || form.getContent().isBlank() || form.getContent().length() > DiagnosisQuestionForm.CONTENT_MAX
                || form.getSortOrder() == null || form.getSortOrder() < 1 || form.optionError() != null) {
            throw new BusinessException(CourseErrorCode.INVALID_INPUT);
        }
        Set<Long> trackIds = form.filledOptions().stream().map(DiagnosisOptionForm::getTrackId).collect(Collectors.toSet());
        if (trackRepository.findAllById(trackIds).size() != trackIds.size()) {
            throw new BusinessException(CourseErrorCode.INVALID_INPUT);
        }
    }

    /* 명세의 실패는 INVALID_INPUT 뿐이라 없는 문항도 INVALID_INPUT 으로 본다 (모의고사·퀴즈 문항과 같은 방식) */
    private DiagnosisQuestion getQuestion(Long diagnosisQuestionId) {
        return diagnosisQuestionRepository.findById(diagnosisQuestionId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.INVALID_INPUT));
    }
}
