package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.policy.ReviewRequestCloser;
import com.vibelog.domain.contest.model.policy.ContestEntryCanceler;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.lesson.model.policy.PortfolioEnrollmentProgress;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.dto.CoursePortfolioCard;
import com.vibelog.domain.portfolio.model.dto.CoursePortfoliosView;
import com.vibelog.domain.portfolio.model.dto.MyPortfolioCard;
import com.vibelog.domain.portfolio.model.dto.PortfolioDetailView;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
 * [PortfolioManageService]
 * 역할 : 내 포트폴리오 목록(API-PRT-05)·삭제(API-PRT-07)·공개 전환(API-PRT-08)·상세(API-PRT-09), U-17 수정 상태 값,
 *        같은 코스 작품 보기(API-PRT-10, P2).
 * 책임 : - 삭제 표시(deleted_at)한 포트폴리오는 없는 것으로 본다 (PORTFOLIO_NOT_FOUND).
 *        - 수정·삭제·공개 전환은 본인만 (NOT_OWNER, NFR-SEC-003).
 *        - 상세는 공개면 회원 누구나, 비공개면 본인만 본다. 남이면 PRIVATE_PORTFOLIO "비공개된 작품입니다" (PL-ERR-006).
 *        - 코스 작품을 삭제하면 같은 트랜잭션에서 lesson 계약 ⑦ recordDeleted 로 그 수강의 완료 일시를 비운다 (PL-ENR-003).
 *        - 작성자가 영구 정지면 본인이 아닌 회원에게 상세를 숨긴다 (HIDDEN_CONTENT, PL-RPT-006·PL-ERR-006, #314).
 *        - 비공개로 바꾸거나 삭제하면 같은 트랜잭션에서 contest 계약 ⑯ cancelEntryOf 로 출품을 취소한다 (PL-CTS-006).
 *        - 비공개로 바꾸거나 삭제하면 같은 트랜잭션에서 community 계약 ⑭ closeOpenRequests 로 그 작품의 열린 리뷰 요청을 닫는다
 *          (FR-REV-005, community.md 결정 8: 다시 공개해도 요청이 되살아나지 않게).
 *        - 코스 이름은 CourseReader, 작성자 닉네임은 MemberReader 로 읽는다.
 *        - 같은 코스 작품 보기는 그 코스에 포트폴리오를 낸 사람(삭제 표시 없음)만 본다 (PL-PRT-011, 아니면 PORTFOLIO_NOT_SUBMITTED).
 *          공개·삭제 표시 없는 작품만, 영구 정지 회원 것은 뺀다 (PL-RPT-006). 최근 제출순 20개씩.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioManageService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioEnrollmentProgress portfolioEnrollmentProgress;
    private final CourseReader courseReader;
    private final MemberReader memberReader;
    private final ReviewRequestCloser reviewRequestCloser;
    private final ContestEntryCanceler contestEntryCanceler;
    private final MessageReader messageReader;

    private static final int COURSE_PORTFOLIO_PAGE_SIZE = 20;   // API-PRT-10 20개씩

    /* U-19 카드 목록 (최근 제출 먼저) */
    public List<MyPortfolioCard> getMyPortfolios(Long memberId) {
        List<Portfolio> portfolios = portfolioRepository.findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPortfolioIdDesc(memberId);
        Map<Long, String> courseTitles = courseReader.getCourseTitles(
                portfolios.stream().map(Portfolio::getCourseId).filter(Objects::nonNull).distinct().toList());
        return portfolios.stream()
                .map(p -> new MyPortfolioCard(p.getPortfolioId(), p.getCourseId(), courseTitles.get(p.getCourseId()),
                        p.getTitle(), p.getSummary(), p.isPublic(), p.getCreatedAt()))
                .toList();
    }

    /* U-20 상세 */
    public PortfolioDetailView getDetail(Long memberId, Long portfolioId) {
        Portfolio p = getActive(portfolioId);
        boolean mine = p.isOwnedBy(memberId);
        if (!mine && !memberReader.findHiddenMemberIds(Set.of(p.getMemberId())).isEmpty()) {
            throw new BusinessException(PortfolioErrorCode.HIDDEN_CONTENT);
        }
        // 신고로 숨긴 작품은 작성자에게만 내용 + 숨김 안내 (FR-RPT-003, message 명세 결정 19, #256). 영구 정지처럼 비공개 검사보다 먼저 본다
        boolean reportHidden = !messageReader.findHiddenTargetIds("PORTFOLIO", List.of(portfolioId)).isEmpty();
        if (reportHidden && !mine) {
            throw new BusinessException(PortfolioErrorCode.HIDDEN_CONTENT);
        }
        if (!p.isPublic() && !mine) {
            throw new BusinessException(PortfolioErrorCode.PRIVATE_PORTFOLIO);
        }
        String courseTitle = p.getCourseId() == null ? null
                : courseReader.getCourseTitles(List.of(p.getCourseId())).get(p.getCourseId());
        return new PortfolioDetailView(p.getPortfolioId(), p.getTitle(), p.getResultUrl(), p.getSummary(),
                p.getRetrospective(), p.getAiTools(), p.getAgentResult(), p.getScreenshotUrl(),
                p.getCourseId(), courseTitle, memberReader.getNickname(p.getMemberId()),
                p.getCreatedAt(), p.isPublic(), mine, reportHidden);
    }

    /* U-35 같은 코스 작품 보기 (API-PRT-10). page 는 화면 그대로 1부터 */
    public CoursePortfoliosView getCoursePortfolios(Long memberId, Long courseId, int page) {
        if (!courseReader.existsById(courseId)) {
            throw new BusinessException(PortfolioErrorCode.COURSE_NOT_FOUND);
        }
        if (!portfolioRepository.existsByMemberIdAndCourseIdAndDeletedAtIsNull(memberId, courseId)) {
            throw new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED);
        }
        Set<Long> hidden = memberReader.findAllHiddenMemberIds();
        Set<Long> hiddenPortfolios = messageReader.findAllHiddenTargetIds("PORTFOLIO");
        // 영구 정지 회원 작품(#215)과 신고로 숨긴 작품(#256)은 뺀다. 없으면 NOT IN () 이 되지 않게 0 (ID 는 1부터)
        Page<Portfolio> portfolios = portfolioRepository.findByCourseIdAndIsPublicTrueAndDeletedAtIsNullAndMemberIdNotInAndPortfolioIdNotIn(
                courseId, hidden.isEmpty() ? Set.of(0L) : hidden, hiddenPortfolios.isEmpty() ? Set.of(0L) : hiddenPortfolios,
                PageRequest.of(Math.max(page, 1) - 1, COURSE_PORTFOLIO_PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("portfolioId"))));
        Map<Long, String> nicknames = memberReader.getNicknames(portfolios.map(Portfolio::getMemberId).toSet());
        return new CoursePortfoliosView(courseId, courseReader.getCourseTitles(List.of(courseId)).get(courseId),
                portfolios.map(p -> new CoursePortfolioCard(p.getPortfolioId(), p.getTitle(), p.getSummary(),
                        nicknames.get(p.getMemberId()), p.getCreatedAt())));
    }

    /* U-17 수정 상태(edit=true)를 채울 값 */
    public PortfolioSubmitForm getEditForm(Long memberId, Long portfolioId) {
        return PortfolioSubmitForm.from(getOwned(memberId, portfolioId));
    }

    /* U-34 개인 작품 수정 상태(?edit=) 값. 코스 포트폴리오는 U-17 에서 고치므로 여기서는 없는 것으로 본다 */
    public PortfolioSubmitForm getPersonalEditForm(Long memberId, Long portfolioId) {
        Portfolio p = getOwned(memberId, portfolioId);
        if (!p.isPersonal()) {
            throw new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
        }
        return PortfolioSubmitForm.from(p);
    }

    /* U-34 는 미인증 회원에게 양식 대신 인증 안내를 보여 준다 (PL-USR-003) */
    public boolean isEmailVerified(Long memberId) {
        return memberReader.isEmailVerified(memberId);
    }

    /* 수정 폼 입력이 틀렸을 때 다시 그릴 U-17 의 코스. 개인 작품이면 null */
    public Long getOwnedCourseId(Long memberId, Long portfolioId) {
        return getOwned(memberId, portfolioId).getCourseId();
    }

    /* API-PRT-07 삭제 표시 + 코스 완료 일시 비움 (계약 ⑦) */
    @Transactional
    public void delete(Long memberId, Long portfolioId) {
        Portfolio p = getOwned(memberId, portfolioId);
        p.markDeleted();
        reviewRequestCloser.closeOpenRequests(portfolioId);
        contestEntryCanceler.cancelEntryOf(portfolioId);
        if (p.getCourseId() != null) {
            portfolioEnrollmentProgress.recordDeleted(memberId, p.getCourseId());
        }
    }

    /* API-PRT-08 공개 전환. 바뀐 값을 돌려준다 */
    @Transactional
    public boolean changeVisibility(Long memberId, Long portfolioId, boolean isPublic) {
        Portfolio p = getOwned(memberId, portfolioId);
        p.changeVisibility(isPublic);
        if (!isPublic) {
            reviewRequestCloser.closeOpenRequests(portfolioId);
            contestEntryCanceler.cancelEntryOf(portfolioId);
        }
        return p.isPublic();
    }

    /* 본인 것만 (수정·삭제·공개 전환). 같은 패키지의 PortfolioSubmitService(수정)도 쓴다 */
    Portfolio getOwned(Long memberId, Long portfolioId) {
        Portfolio p = getActive(portfolioId);
        if (!p.isOwnedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        return p;
    }

    private Portfolio getActive(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }
}
