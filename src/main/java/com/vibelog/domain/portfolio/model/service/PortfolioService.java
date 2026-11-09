package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.policy.PortfolioAccess;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.lesson.model.service.LessonReader;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.GuideStepCheckRepository;
import com.vibelog.domain.portfolio.model.dao.GuideStepRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioFaqRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckResult;
import com.vibelog.domain.portfolio.model.dto.PortfolioPageView;
import com.vibelog.domain.portfolio.model.entity.GuideStep;
import com.vibelog.domain.portfolio.model.entity.GuideStepCheck;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [PortfolioService]
 * 역할 : 학습자의 포트폴리오 과제 화면(API-PRT-01)·스텝 체크(API-PRT-02)·설정 파일 내려받기(API-PRT-03).
 * 책임 : - 과제가 열렸는지는 lesson 영역 계약 ⑥(getAccess)으로 묻는다. 수강 안 함이면 화면은 코스 소개로 보내고,
 *          스텝 체크·내려받기는 PORTFOLIO_LOCKED 로 막는다 (명세 실패 칸).
 *        - 스텝은 사용 중(is_active)인 것만 보여 주고 센다. 꺼진 스텝은 체크할 수 없다.
 *        - 코스 이름·마지막 수업·이메일 인증 여부는 각 영역 Reader 로 읽는다.
 *        - 과제 FAQ(P2 FR-PRT-008)는 과제가 열렸을 때만 보낸다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioEnrollmentProgress portfolioEnrollmentProgress;
    private final PortfolioAssignmentRepository portfolioAssignmentRepository;
    private final GuideStepRepository guideStepRepository;
    private final GuideStepCheckRepository guideStepCheckRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioFaqRepository portfolioFaqRepository;
    private final CourseReader courseReader;
    private final LessonReader lessonReader;
    private final MemberReader memberReader;

    /* U-17 화면 값. 수강하지 않은 코스면 비어 있다 (Controller 가 코스 소개로 보낸다) */
    public Optional<PortfolioPageView> getPage(Long memberId, Long courseId) {
        PortfolioAccess access = portfolioEnrollmentProgress.getAccess(memberId, courseId);
        if (!access.enrolled()) {
            return Optional.empty();
        }
        String courseTitle = courseReader.getCourseTitles(List.of(courseId)).get(courseId);
        if (!access.unlocked()) {
            return Optional.of(PortfolioPageView.locked(courseId, courseTitle, lessonReader.getLastLessonId(courseId)));
        }

        PortfolioAssignment assignment = getAssignment(courseId);
        List<GuideStep> steps = activeSteps(assignment);
        Set<Long> checked = checkedStepIds(memberId, steps);
        List<PortfolioPageView.Step> stepViews = steps.stream()
                .map(step -> new PortfolioPageView.Step(step.getGuideStepId(), step.getContent(),
                        checked.contains(step.getGuideStepId())))
                .toList();
        Long submittedPortfolioId = portfolioRepository.findFirstByMemberIdAndCourseIdAndDeletedAtIsNull(memberId, courseId)
                .map(Portfolio::getPortfolioId)
                .orElse(null);

        return Optional.of(new PortfolioPageView(courseId, courseTitle, true, null,
                assignment.getTopic(), assignment.getGoal(), assignment.getAgentGuide(), assignment.getAgentFileUrl() != null,
                stepViews, checked.size(), submittedPortfolioId, memberReader.isEmailVerified(memberId),
                portfolioFaqRepository.findByPortfolioAssignmentIdOrderBySortOrderAsc(assignment.getPortfolioAssignmentId()).stream()
                        .map(faq -> new PortfolioPageView.Faq(faq.getQuestion(), faq.getAnswer()))
                        .toList()));
    }

    /* API-PRT-02 체크 저장·삭제. 같은 값을 두 번(또는 동시에) 보내도 결과가 같다 (행이 두 번 생기지 않음) */
    @Transactional
    public GuideStepCheckResult check(Long memberId, Long guideStepId, boolean checked) {
        GuideStep step = guideStepRepository.findById(guideStepId)
                .filter(GuideStep::isActive)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
        PortfolioAssignment assignment = portfolioAssignmentRepository.findById(step.getPortfolioAssignmentId())
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
        requireUnlocked(memberId, assignment.getCourseId());

        if (checked) {
            guideStepCheckRepository.insertIfAbsent(memberId, guideStepId);
        } else {
            guideStepCheckRepository.deleteByMemberIdAndGuideStepId(memberId, guideStepId);
        }

        List<GuideStep> steps = activeSteps(assignment);
        return new GuideStepCheckResult(checkedStepIds(memberId, steps).size(), steps.size());
    }

    /* API-PRT-03 내려받을 설정 파일 주소 (FileStorage 가 돌려준 /uploads/...) */
    public String getAgentFileUrl(Long memberId, Long courseId) {
        requireUnlocked(memberId, courseId);
        String url = getAssignment(courseId).getAgentFileUrl();
        if (url == null) {
            throw new BusinessException(PortfolioErrorCode.FILE_NOT_FOUND);
        }
        return url;
    }

    private void requireUnlocked(Long memberId, Long courseId) {
        if (!portfolioEnrollmentProgress.getAccess(memberId, courseId).unlocked()) {
            throw new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED);
        }
    }

    /* 코스는 과제가 있어야 공개된다(계약 ⑨). 그래도 없으면 찾을 수 없음 */
    private PortfolioAssignment getAssignment(Long courseId) {
        return portfolioAssignmentRepository.findByCourseId(courseId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private List<GuideStep> activeSteps(PortfolioAssignment assignment) {
        return guideStepRepository.findByPortfolioAssignmentIdAndIsActiveTrueOrderBySortOrderAsc(
                assignment.getPortfolioAssignmentId());
    }

    private Set<Long> checkedStepIds(Long memberId, List<GuideStep> steps) {
        if (steps.isEmpty()) {
            return Set.of();
        }
        List<Long> stepIds = steps.stream().map(GuideStep::getGuideStepId).toList();
        return guideStepCheckRepository.findByMemberIdAndGuideStepIdIn(memberId, stepIds).stream()
                .map(GuideStepCheck::getGuideStepId)
                .collect(Collectors.toSet());
    }
}
