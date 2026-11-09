package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.GuideStepCheckRepository;
import com.vibelog.domain.portfolio.model.dao.GuideStepRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioFaqRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioAgentForm;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentAdminView;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentForm;
import com.vibelog.domain.portfolio.model.dto.PortfolioFaqForm;
import com.vibelog.domain.portfolio.model.entity.GuideStep;
import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import com.vibelog.domain.portfolio.model.entity.PortfolioFaq;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.file.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [PortfolioAdminService]
 * 역할 : 운영자 포트폴리오 과제 편집 (A-07, API-ADM-34~36, FR-PRT-012·013), 과제 FAQ (API-PRT-11, FR-PRT-014, P2).
 * 책임 : - 코스는 다른 영역이라 CourseReader 로 이름을 받고, 없으면 COURSE_NOT_FOUND.
 *        - 스텝은 보낸 목록으로 맞춘다. 목록에서 빠진 스텝은 학습자 체크 기록이 있으면 끄고(is_active), 없으면 지운다.
 *        - 설정 파일 검사(md·json·yaml·txt, 5MB, 실행 파일 불가, PL-PRT-010)는 FileStorage 가 한다.
 *        - FAQ 도 보낸 목록으로 맞춘다. 빠진 FAQ 는 행을 지운다 (운영자 안내 문구라 삭제 표시 없음, portfolio.md P2).
 *          과제를 저장하기 전이거나 다른 과제의 portfolioFaqId 면 INVALID_INPUT.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioAdminService {

    private final PortfolioAssignmentRepository portfolioAssignmentRepository;
    private final GuideStepRepository guideStepRepository;
    private final GuideStepCheckRepository guideStepCheckRepository;
    private final PortfolioFaqRepository portfolioFaqRepository;
    private final CourseReader courseReader;
    private final FileStorage fileStorage;

    /* API-ADM-34 화면 머리 정보 */
    @Transactional(readOnly = true)
    public PortfolioAssignmentAdminView getView(Long courseId) {
        String courseTitle = getCourseTitle(courseId);
        Optional<PortfolioAssignment> assignment = portfolioAssignmentRepository.findByCourseId(courseId);
        return new PortfolioAssignmentAdminView(courseId, courseTitle, assignment.isPresent(),
                assignment.map(PortfolioAssignment::getAgentFileUrl).orElse(null));
    }

    /* API-ADM-34 과제·스텝 탭 폼 값. 과제가 없으면 빈 폼 */
    @Transactional(readOnly = true)
    public PortfolioAssignmentForm getAssignmentForm(Long courseId) {
        return portfolioAssignmentRepository.findByCourseId(courseId)
                .map(assignment -> PortfolioAssignmentForm.of(assignment.getTopic(), assignment.getGoal(),
                        activeStepsOf(assignment).stream()
                                .map(step -> new PortfolioAssignmentForm.Step(step.getGuideStepId(), step.getContent()))
                                .toList()))
                .orElseGet(PortfolioAssignmentForm::new);
    }

    /* API-ADM-34 평가 에이전트 탭 폼 값 */
    @Transactional(readOnly = true)
    public PortfolioAgentForm getAgentForm(Long courseId) {
        return new PortfolioAgentForm(portfolioAssignmentRepository.findByCourseId(courseId)
                .map(PortfolioAssignment::getAgentGuide)
                .orElse(""));
    }

    /* A-07 FAQ 탭 폼 값 (API-ADM-34 tab=faq). 과제가 없으면 빈 폼 */
    @Transactional(readOnly = true)
    public PortfolioFaqForm getFaqForm(Long courseId) {
        return PortfolioFaqForm.of(portfolioAssignmentRepository.findByCourseId(courseId)
                .map(assignment -> portfolioFaqRepository.findByPortfolioAssignmentIdOrderBySortOrderAsc(assignment.getPortfolioAssignmentId())
                        .stream()
                        .map(faq -> new PortfolioFaqForm.Faq(faq.getPortfolioFaqId(), faq.getQuestion(), faq.getAnswer()))
                        .toList())
                .orElse(List.of()));
    }

    /* API-PRT-11. 보낸 목록으로 맞춘다: 있는 ID 는 고치고, 새 항목은 추가하고, 목록에 없는 기존 FAQ 는 지운다 */
    public void saveFaqs(Long courseId, PortfolioFaqForm form) {
        getCourseTitle(courseId);
        PortfolioAssignment assignment = portfolioAssignmentRepository.findByCourseId(courseId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.INVALID_INPUT));
        Map<Long, PortfolioFaq> current = portfolioFaqRepository
                .findByPortfolioAssignmentIdOrderBySortOrderAsc(assignment.getPortfolioAssignmentId()).stream()
                .collect(Collectors.toMap(PortfolioFaq::getPortfolioFaqId, Function.identity()));
        List<PortfolioFaqForm.Faq> faqs = form.getFaqs();
        for (int i = 0; i < faqs.size(); i++) {
            PortfolioFaqForm.Faq faq = faqs.get(i);
            int sortOrder = i + 1;
            if (faq.getPortfolioFaqId() == null) {
                portfolioFaqRepository.save(PortfolioFaq.create(assignment.getPortfolioAssignmentId(), faq.getQuestion(),
                        faq.getAnswer(), sortOrder));
                continue;
            }
            PortfolioFaq found = current.remove(faq.getPortfolioFaqId());
            if (found == null) {
                throw new BusinessException(PortfolioErrorCode.INVALID_INPUT);
            }
            found.edit(faq.getQuestion(), faq.getAnswer(), sortOrder);
        }
        portfolioFaqRepository.deleteAll(current.values());
    }

    /*
     * API-ADM-35. 과제가 없으면 만들고(평가 에이전트 안내는 비워 두고 API-ADM-36 에서 쓴다), 있으면 고친다.
     * 이 과제의 사용 중인 스텝이 아닌 guideStepId 가 오면 INVALID_INPUT.
     */
    public void saveAssignment(Long courseId, PortfolioAssignmentForm form) {
        getCourseTitle(courseId);
        PortfolioAssignment assignment = portfolioAssignmentRepository.findByCourseId(courseId)
                .map(found -> {
                    found.editAssignment(form.getTopic(), form.getGoal());
                    return found;
                })
                .orElseGet(() -> portfolioAssignmentRepository.save(
                        PortfolioAssignment.create(courseId, form.getTopic(), form.getGoal(), "", null)));

        Map<Long, GuideStep> current = activeStepsOf(assignment).stream()
                .collect(Collectors.toMap(GuideStep::getGuideStepId, Function.identity()));
        List<PortfolioAssignmentForm.Step> steps = form.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            PortfolioAssignmentForm.Step step = steps.get(i);
            int sortOrder = i + 1;
            if (step.getGuideStepId() == null) {
                guideStepRepository.save(GuideStep.create(assignment.getPortfolioAssignmentId(), step.getContent(), sortOrder));
                continue;
            }
            GuideStep found = current.remove(step.getGuideStepId());
            if (found == null) {
                throw new BusinessException(PortfolioErrorCode.INVALID_INPUT);
            }
            found.edit(step.getContent(), sortOrder);
        }
        current.values().forEach(this::removeStep);
    }

    /* API-ADM-36. 과제를 먼저 저장해야 한다. 새 파일이 없으면 올려 둔 파일을 그대로 둔다 */
    public void saveAgent(Long courseId, String agentGuide, MultipartFile agentFile) {
        getCourseTitle(courseId);
        PortfolioAssignment assignment = portfolioAssignmentRepository.findByCourseId(courseId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.INVALID_INPUT));
        String fileUrl = agentFile == null || agentFile.isEmpty() ? null : fileStorage.saveDocument(agentFile);
        assignment.editAgent(agentGuide, fileUrl);
    }

    private void removeStep(GuideStep step) {
        if (guideStepCheckRepository.existsByGuideStepId(step.getGuideStepId())) {
            step.deactivate();
        } else {
            guideStepRepository.delete(step);
        }
    }

    private List<GuideStep> activeStepsOf(PortfolioAssignment assignment) {
        return guideStepRepository.findByPortfolioAssignmentIdAndIsActiveTrueOrderBySortOrderAsc(
                assignment.getPortfolioAssignmentId());
    }

    private String getCourseTitle(Long courseId) {
        String title = courseReader.getCourseTitles(List.of(courseId)).get(courseId);
        if (title == null) {
            throw new BusinessException(PortfolioErrorCode.COURSE_NOT_FOUND);
        }
        return title;
    }
}
