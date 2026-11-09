package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.file.FileStorage;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.vibelog.domain.portfolio.model.policy.ApiKeyMasker.mask;

/*
 * [PortfolioSubmitService]
 * 역할 : 코스 과제 포트폴리오 제출 (API-PRT-04, FR-PRT-003·004)과 수정 (API-PRT-06, FR-PRT-006).
 * 책임 : - 순서: 과제 열림(계약 ⑥) → 이메일 인증(PL-USR-003) → 이미 제출함 → 금칙어(PL-SEC-003) → API 키 가리기(PL-PRT-008)
 *          → 스크린샷 저장(1장, PL-PRT-005·_template.md 10장, 형식·크기는 FileStorage) → 저장 → 계약 ⑦ recordSubmitted.
 *        - 이미 제출함(ALREADY_SUBMITTED)은 삭제 표시가 없는 포트폴리오가 있을 때만이다. 삭제 표시한 것이 있으면 그 행을 되살려
 *          덮어쓴다 (_template.md 9-2, DB 설계서 5-4).
 *        - 코스 완료 기록(계약 ⑦)은 같은 트랜잭션이라, 저장이 실패하면 완료도 남지 않는다.
 *        - API 키는 글 칸(제목·요약·회고·AI 도구·에이전트 점검 결과)에서 가린다. 결과물 링크는 URL 이라 가리지 않는다.
 *        - 에이전트 점검 결과는 명세에 길이가 없어 저장 칸 크기(agent_result TEXT, 65,535바이트)를 가린 뒤의 값으로 검사한다.
 *          넘으면 INVALID_INPUT (Controller 가 그 칸의 메시지로 바꾼다).
 *        - 수정은 제출과 같은 폼·같은 검사(인증·금칙어·API 키·길이)를 하고 최신본으로 덮는다 (PL-PRT-007).
 *          본인 것이 아니면 NOT_OWNER. 스크린샷은 새로 올렸을 때만 바꾼다. 콘테스트 출품 중 수정 막기(CONTEST_ENTERED)는 P2.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioSubmitService {

    private final PortfolioEnrollmentProgress portfolioEnrollmentProgress;
    private final PortfolioRepository portfolioRepository;
    private final MemberReader memberReader;
    private final BannedWordPolicy bannedWordPolicy;
    private final FileStorage fileStorage;
    private final PortfolioManageService portfolioManageService;

    private static final int AGENT_RESULT_MAX_BYTES = 65_535;   // sql/01-schema.sql portfolio.agent_result TEXT

    @Transactional
    public void submit(Long memberId, Long courseId, PortfolioSubmitForm form, MultipartFile screenshot) {
        if (!portfolioEnrollmentProgress.getAccess(memberId, courseId).unlocked()) {
            throw new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED);
        }
        requireEmailVerified(memberId);
        Optional<Portfolio> existing = portfolioRepository.findByMemberIdAndCourseId(memberId, courseId);
        if (existing.isPresent() && !existing.get().isDeleted()) {
            throw new BusinessException(PortfolioErrorCode.ALREADY_SUBMITTED);
        }
        String agentResult = checkAndMaskAgentResult(form);
        String screenshotUrl = saveScreenshot(screenshot);
        if (existing.isPresent()) {
            existing.get().resubmit(mask(form.getTitle()), form.getResultUrl(), mask(form.getSummary()),
                    mask(form.getRetrospective()), mask(form.getAiTools()), form.getIsPublic(), agentResult, screenshotUrl);
        } else {
            portfolioRepository.save(Portfolio.create(memberId, courseId, mask(form.getTitle()), form.getResultUrl(),
                    mask(form.getSummary()), mask(form.getRetrospective()), mask(form.getAiTools()), form.getIsPublic(),
                    agentResult, screenshotUrl));
        }
        portfolioEnrollmentProgress.recordSubmitted(memberId, courseId);
    }

    /* API-PRT-06 수정: 최신본으로 덮는다 */
    @Transactional
    public void edit(Long memberId, Long portfolioId, PortfolioSubmitForm form, MultipartFile screenshot) {
        Portfolio portfolio = portfolioManageService.getOwned(memberId, portfolioId);
        requireEmailVerified(memberId);
        String agentResult = checkAndMaskAgentResult(form);
        portfolio.edit(mask(form.getTitle()), form.getResultUrl(), mask(form.getSummary()), mask(form.getRetrospective()),
                mask(form.getAiTools()), form.getIsPublic(), agentResult, saveScreenshot(screenshot));
    }

    private void requireEmailVerified(Long memberId) {
        if (!memberReader.isEmailVerified(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /* 금칙어 검사 후 에이전트 점검 결과를 가리고 저장 칸 크기를 본다. 비었으면 null */
    private String checkAndMaskAgentResult(PortfolioSubmitForm form) {
        bannedWordPolicy.check(form.getTitle(), form.getSummary(), form.getRetrospective(), form.getAiTools(),
                form.getAgentResult());
        String agentResult = form.getAgentResult() == null || form.getAgentResult().isBlank()
                ? null : mask(form.getAgentResult());
        if (agentResult != null && agentResult.getBytes(StandardCharsets.UTF_8).length > AGENT_RESULT_MAX_BYTES) {
            throw new BusinessException(PortfolioErrorCode.INVALID_INPUT);
        }
        return agentResult;
    }

    private String saveScreenshot(MultipartFile screenshot) {
        return screenshot == null || screenshot.isEmpty() ? null : fileStorage.saveImage(screenshot);
    }
}
