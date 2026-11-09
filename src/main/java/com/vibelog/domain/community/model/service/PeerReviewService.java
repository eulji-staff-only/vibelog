package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.PeerReviewRepository;
import com.vibelog.domain.community.model.dao.ReviewCommentRepository;
import com.vibelog.domain.community.model.dao.ReviewCriterionRepository;
import com.vibelog.domain.community.model.dao.ReviewRequestRepository;
import com.vibelog.domain.community.model.dto.MyReviewItem;
import com.vibelog.domain.community.model.dto.ReviewCommentInput;
import com.vibelog.domain.community.model.dto.ReviewField;
import com.vibelog.domain.community.model.dto.ReviewFormView;
import com.vibelog.domain.community.model.dto.ReviewRequestCard;
import com.vibelog.domain.community.model.dto.ReviewView;
import com.vibelog.domain.community.model.entity.PeerReview;
import com.vibelog.domain.community.model.entity.ReviewComment;
import com.vibelog.domain.community.model.entity.ReviewCriterion;
import com.vibelog.domain.community.model.entity.ReviewRequest;
import com.vibelog.domain.course.model.service.CourseReader;
import com.vibelog.domain.message.model.policy.NotificationSender;
import com.vibelog.domain.portfolio.model.dto.PortfolioBrief;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [PeerReviewService]
 * 역할 : 동료 리뷰 요청·마감, 요청 목록과 리뷰 쓰기·수정·삭제·도움됨 (API-REV-01~07, FR-REV-001~005, PL-REV-001~003).
 * 책임 : - 열린 요청 = 마감 안 됐고 14일 안이고 작품이 공개 중 (PL-REV-002). 비공개·삭제는 portfolio 영역이 계약 ⑭로 닫지만,
 *          그 전에 들어온 요청도 닫힌 것으로 본다.
 *        - 요청은 작품 주인만, 공개 작품만, 작품당 열린 요청 1개, 내 열린 요청 3개까지.
 *        - 리뷰는 한 요청에 1인 1리뷰 (PL-REV-003). 지운 리뷰를 다시 쓰면 그 행을 되살린다. 수정은 마감 뒤에도 된다.
 *        - 보낸 기준 묶음이 그 과제의 활성 기준과 정확히 같아야 한다 (빠짐·중복·다른 과제 → INVALID_INPUT).
 *          기준이 없는 과제·개인 작품은 기준 없는 코멘트 1개.
 *        - 리뷰를 쓰면 요청자에게 REVIEW 알림 (계약 ⑬). 끝나면 작품 상세로, 작품이 비공개·삭제됐으면 /reviews 로 보낸다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeerReviewService {

    static final int PAGE_SIZE = 20;
    static final int MAX_OPEN_REQUESTS = 3;
    public static final String REVIEWS_URL = "/reviews";

    private final ReviewRequestRepository reviewRequestRepository;
    private final PeerReviewRepository peerReviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewCriterionRepository reviewCriterionRepository;
    private final PortfolioReader portfolioReader;
    private final CourseReader courseReader;
    private final MemberReader memberReader;
    private final BannedWordPolicy bannedWordPolicy;
    private final NotificationSender notificationSender;

    /* API-REV-01 */
    @Transactional
    public void request(Long memberId, Long portfolioId) {
        // 같은 회원의 요청을 차례로 처리한다 (버튼 두 번·탭 두 개로 열린 요청이 두 개 생기지 않게)
        List<Long> myPortfolioIds = portfolioReader.lockLivePortfolioIdsOf(memberId);
        PortfolioBrief portfolio = portfolioReader.findBrief(portfolioId).filter(p -> !p.deleted())
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.PORTFOLIO_NOT_FOUND));
        if (!portfolio.memberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        if (!portfolio.isPublic()) {
            throw new BusinessException(CommunityErrorCode.PRIVATE_PORTFOLIO);
        }
        if (reviewRequestRepository.findFirstByPortfolioIdAndClosedAtIsNullAndRequestedAtAfterOrderByReviewRequestIdDesc(
                portfolioId, openSince()).isPresent()) {
            throw new BusinessException(CommunityErrorCode.REVIEW_REQUEST_OPEN);
        }
        if (reviewRequestRepository.countByPortfolioIdInAndClosedAtIsNullAndRequestedAtAfter(myPortfolioIds, openSince())
                >= MAX_OPEN_REQUESTS) {
            throw new BusinessException(CommunityErrorCode.REVIEW_REQUEST_LIMIT);
        }
        reviewRequestRepository.save(ReviewRequest.create(portfolioId));
    }

    /* API-REV-07. 작품 ID 를 돌려준다 (작품 상세로 돌아간다) */
    @Transactional
    public Long close(Long memberId, Long reviewRequestId) {
        ReviewRequest request = findRequest(reviewRequestId);
        PortfolioBrief portfolio = briefOf(request);
        if (!portfolio.memberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        if (!request.isOpen(LocalDateTime.now())) {
            throw new BusinessException(CommunityErrorCode.REVIEW_REQUEST_CLOSED);
        }
        request.close();
        return request.getPortfolioId();
    }

    /* API-REV-02 열린 요청 카드, 최근 요청순 */
    public Page<ReviewRequestCard> getOpenRequests(int page) {
        Page<ReviewRequest> requests = reviewRequestRepository.findByClosedAtIsNullAndRequestedAtAfterOrderByRequestedAtDescReviewRequestIdDesc(
                openSince(), PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE));
        Map<Long, PortfolioBrief> portfolios = portfolioReader.findBriefs(requests.map(ReviewRequest::getPortfolioId).toSet());
        Map<Long, String> courseTitles = courseReader.getCourseTitles(
                portfolios.values().stream().map(PortfolioBrief::courseId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> nicknames = memberReader.getNicknames(
                portfolios.values().stream().map(PortfolioBrief::memberId).collect(Collectors.toSet()));
        Map<Long, Long> reviewCounts = countReviews(requests.map(ReviewRequest::getReviewRequestId).toList());
        // ponytail: 계약 ⑭ 전에 비공개·삭제된 작품은 이 쪽에서 빼서 한 쪽이 20개보다 적을 수 있다. ⑭가 붙으면 생기지 않는다
        List<ReviewRequestCard> cards = requests.stream()
                .filter(request -> visible(portfolios.get(request.getPortfolioId())))
                .map(request -> {
                    PortfolioBrief p = portfolios.get(request.getPortfolioId());
                    return new ReviewRequestCard(request.getReviewRequestId(), p.portfolioId(), p.title(), p.summary(),
                            p.courseId() == null ? null : courseTitles.get(p.courseId()), nicknames.get(p.memberId()),
                            request.getRequestedAt(), reviewCounts.getOrDefault(request.getReviewRequestId(), 0L));
                })
                .toList();
        return new PageImpl<>(cards, requests.getPageable(), requests.getTotalElements());
    }

    /* API-REV-02 내가 쓴 리뷰 (myPage). 작품이 비공개·삭제돼도 고치고 지울 수 있게 보여 준다 (PL-REV-003) */
    public Page<MyReviewItem> getMyReviews(Long memberId, int myPage) {
        Page<PeerReview> reviews = peerReviewRepository.findByReviewerMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPeerReviewIdDesc(
                memberId, PageRequest.of(Math.max(myPage, 1) - 1, PAGE_SIZE));
        Map<Long, ReviewRequest> requests = reviewRequestRepository.findAllById(reviews.map(PeerReview::getReviewRequestId).toSet())
                .stream().collect(Collectors.toMap(ReviewRequest::getReviewRequestId, Function.identity()));
        Map<Long, PortfolioBrief> portfolios = portfolioReader.findBriefs(
                requests.values().stream().map(ReviewRequest::getPortfolioId).collect(Collectors.toSet()));
        return reviews.map(review -> new MyReviewItem(review.getPeerReviewId(), review.getReviewRequestId(),
                titleFor(portfolios.get(requests.get(review.getReviewRequestId()).getPortfolioId())), review.getCreatedAt()));
    }

    /* API-REV-02 reviewRequestId: 쓰기 폼, 내 리뷰가 있으면 (마감됐어도) 수정 폼 */
    public ReviewFormView getForm(Long memberId, Long reviewRequestId) {
        ReviewRequest request = findRequest(reviewRequestId);
        PortfolioBrief portfolio = briefOf(request);
        if (portfolio.memberId().equals(memberId)) {
            throw new BusinessException(CommunityErrorCode.SELF_REVIEW);
        }
        Optional<PeerReview> mine = peerReviewRepository.findByReviewRequestIdAndReviewerMemberId(reviewRequestId, memberId)
                .filter(review -> !review.isDeleted());
        if (mine.isEmpty() && !isOpen(request, portfolio)) {
            throw new BusinessException(CommunityErrorCode.REVIEW_REQUEST_CLOSED);
        }
        Map<Long, String> contents = new HashMap<>();
        mine.ifPresent(review -> reviewCommentRepository.findByPeerReviewIdInOrderByReviewCommentIdAsc(List.of(review.getPeerReviewId()))
                .forEach(comment -> contents.put(comment.getReviewCriterionId(), comment.getContent())));
        List<ReviewCriterion> criteria = activeCriteria(portfolio);
        List<ReviewField> fields = criteria.isEmpty()
                ? List.of(new ReviewField(null, null, null, contents.get(null)))
                : criteria.stream().map(c -> new ReviewField(c.getReviewCriterionId(), c.getName(), c.getDescription(),
                        contents.get(c.getReviewCriterionId()))).toList();
        return new ReviewFormView(reviewRequestId, portfolio.portfolioId(), titleFor(portfolio),
                mine.map(PeerReview::getPeerReviewId).orElse(null), fields);
    }

    /* API-REV-03. 끝나고 갈 주소를 돌려준다 */
    @Transactional
    public String write(Long memberId, Long reviewRequestId, List<ReviewCommentInput> comments) {
        ReviewRequest request = findRequest(reviewRequestId);
        PortfolioBrief portfolio = briefOf(request);
        checkCriteria(portfolio, comments);
        checkBannedWords(comments);
        if (!memberReader.isEmailVerified(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (portfolio.memberId().equals(memberId)) {
            throw new BusinessException(CommunityErrorCode.SELF_REVIEW);
        }
        Optional<PeerReview> existing = peerReviewRepository.findByReviewRequestIdAndReviewerMemberId(reviewRequestId, memberId);
        if (existing.isPresent() && !existing.get().isDeleted()) {
            throw new BusinessException(CommunityErrorCode.ALREADY_REVIEWED);
        }
        if (!isOpen(request, portfolio)) {
            throw new BusinessException(CommunityErrorCode.REVIEW_REQUEST_CLOSED);
        }
        Long peerReviewId = existing.map(review -> {
            review.revive();
            reviewCommentRepository.deleteByPeerReviewId(review.getPeerReviewId()); // 되살린 상태를 먼저 flush 한다
            return review.getPeerReviewId();
        }).orElseGet(() -> peerReviewRepository.save(PeerReview.create(reviewRequestId, memberId)).getPeerReviewId());
        saveComments(peerReviewId, comments);
        notificationSender.reviewed(memberId, portfolio.memberId(), peerReviewId, portfolio.title());
        return destination(portfolio);
    }

    /* API-REV-04. 마감돼도 고칠 수 있다 */
    @Transactional
    public String edit(Long memberId, Long peerReviewId, List<ReviewCommentInput> comments) {
        PeerReview review = myReview(memberId, peerReviewId);
        PortfolioBrief portfolio = briefOf(findRequest(review.getReviewRequestId()));
        checkCriteria(portfolio, comments);
        checkBannedWords(comments);
        reviewCommentRepository.deleteByPeerReviewId(peerReviewId);
        saveComments(peerReviewId, comments);
        return destination(portfolio);
    }

    /* API-REV-05 */
    @Transactional
    public String delete(Long memberId, Long peerReviewId) {
        PeerReview review = myReview(memberId, peerReviewId);
        review.delete();
        return destination(briefOf(findRequest(review.getReviewRequestId())));
    }

    /* API-REV-06. 요청자(작품 주인)만. 바꾼 뒤 상태를 돌려준다 */
    @Transactional
    public boolean toggleHelpful(Long memberId, Long peerReviewId) {
        PeerReview review = peerReviewRepository.findById(peerReviewId).filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_OWNER));
        if (!briefOf(findRequest(review.getReviewRequestId())).memberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        return review.toggleHelpful();
    }

    /* 마감 실패 때도 작품 상세로 돌아가려고 요청의 작품 ID 를 먼저 찾는다 */
    public Long getPortfolioIdOfRequest(Long reviewRequestId) {
        return findRequest(reviewRequestId).getPortfolioId();
    }

    /* 수정 입력 오류로 폼을 다시 그릴 때, 내 리뷰의 요청 ID */
    public Long getReviewRequestIdOf(Long memberId, Long peerReviewId) {
        return myReview(memberId, peerReviewId).getReviewRequestId();
    }

    /* CommunityReader: 알림 도착지 (message API-NTF-02). 지운 리뷰도 */
    public Optional<Long> findPortfolioIdOfReview(Long peerReviewId) {
        return peerReviewRepository.findById(peerReviewId)
                .flatMap(review -> reviewRequestRepository.findById(review.getReviewRequestId()))
                .map(ReviewRequest::getPortfolioId);
    }

    /* CommunityReader: U-20 받은 리뷰 (모든 요청의 리뷰, 오래된 순) */
    public List<ReviewView> findReviewsOfPortfolio(Long portfolioId, Long viewerMemberId) {
        List<Long> requestIds = reviewRequestRepository.findByPortfolioId(portfolioId).stream().map(ReviewRequest::getReviewRequestId).toList();
        if (requestIds.isEmpty()) {
            return List.of();
        }
        List<PeerReview> reviews = peerReviewRepository.findByReviewRequestIdInAndDeletedAtIsNullOrderByCreatedAtAscPeerReviewIdAsc(requestIds);
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<ReviewComment> comments = reviewCommentRepository.findByPeerReviewIdInOrderByReviewCommentIdAsc(
                reviews.stream().map(PeerReview::getPeerReviewId).toList());
        Set<Long> criterionIds = comments.stream().map(ReviewComment::getReviewCriterionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> criterionNames = criterionIds.isEmpty() ? Map.of() : reviewCriterionRepository.findByReviewCriterionIdIn(criterionIds)
                .stream().collect(Collectors.toMap(ReviewCriterion::getReviewCriterionId, ReviewCriterion::getName));
        Map<Long, List<ReviewView.Comment>> commentsByReview = comments.stream().collect(Collectors.groupingBy(
                ReviewComment::getPeerReviewId, Collectors.mapping(c -> new ReviewView.Comment(
                        c.getReviewCriterionId() == null ? null : criterionNames.get(c.getReviewCriterionId()), c.getContent()),
                        Collectors.toList())));
        Map<Long, String> nicknames = memberReader.getNicknames(reviews.stream().map(PeerReview::getReviewerMemberId).collect(Collectors.toSet()));
        return reviews.stream().map(review -> new ReviewView(review.getPeerReviewId(), review.getReviewerMemberId(),
                nicknames.get(review.getReviewerMemberId()), review.getCreatedAt(), review.getHelpfulAt() != null,
                review.isWrittenBy(viewerMemberId), commentsByReview.getOrDefault(review.getPeerReviewId(), List.of()))).toList();
    }

    /* CommunityReader: U-20 리뷰 마감 버튼 */
    public Optional<Long> findOpenReviewRequestId(Long portfolioId) {
        return reviewRequestRepository.findFirstByPortfolioIdAndClosedAtIsNullAndRequestedAtAfterOrderByReviewRequestIdDesc(portfolioId, openSince())
                .map(ReviewRequest::getReviewRequestId);
    }

    /* CommunityReader: U-19 카드의 리뷰 요청 중 표시 */
    public Set<Long> findPortfolioIdsWithOpenRequest(Collection<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) {
            return Set.of();
        }
        return reviewRequestRepository.findByPortfolioIdInAndClosedAtIsNullAndRequestedAtAfter(portfolioIds, openSince()).stream()
                .map(ReviewRequest::getPortfolioId).collect(Collectors.toSet());
    }

    private void checkCriteria(PortfolioBrief portfolio, List<ReviewCommentInput> comments) {
        List<Long> expected = activeCriteria(portfolio).stream().map(ReviewCriterion::getReviewCriterionId).toList();
        List<Long> sent = comments.stream().map(ReviewCommentInput::getReviewCriterionId).toList();
        Set<Long> sentSet = new HashSet<>(sent);
        boolean matches = expected.isEmpty()
                ? sent.size() == 1 && sent.get(0) == null
                : sent.size() == expected.size() && sentSet.size() == sent.size() && sentSet.equals(new HashSet<>(expected));
        if (!matches) {
            throw new BusinessException(CommunityErrorCode.INVALID_INPUT);
        }
    }

    private void checkBannedWords(List<ReviewCommentInput> comments) {
        bannedWordPolicy.check(comments.stream().map(ReviewCommentInput::getContent).toArray(String[]::new));
    }

    private void saveComments(Long peerReviewId, List<ReviewCommentInput> comments) {
        reviewCommentRepository.saveAll(comments.stream()
                .map(c -> ReviewComment.create(peerReviewId, c.getReviewCriterionId(), c.getContent().strip()))
                .toList());
    }

    private List<ReviewCriterion> activeCriteria(PortfolioBrief portfolio) {
        return portfolio.assignmentId() == null ? List.of() : reviewCriterionRepository.findActiveOf(portfolio.assignmentId());
    }

    private Map<Long, Long> countReviews(List<Long> reviewRequestIds) {
        if (reviewRequestIds.isEmpty()) {
            return Map.of();
        }
        return peerReviewRepository.countLiveByRequestIds(reviewRequestIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private PeerReview myReview(Long memberId, Long peerReviewId) {
        return peerReviewRepository.findById(peerReviewId)
                .filter(review -> !review.isDeleted() && review.isWrittenBy(memberId))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_OWNER));
    }

    private ReviewRequest findRequest(Long reviewRequestId) {
        return reviewRequestRepository.findById(reviewRequestId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.REVIEW_REQUEST_NOT_FOUND));
    }

    private PortfolioBrief briefOf(ReviewRequest request) {
        return portfolioReader.findBrief(request.getPortfolioId())
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.REVIEW_REQUEST_NOT_FOUND));
    }

    private boolean isOpen(ReviewRequest request, PortfolioBrief portfolio) {
        return request.isOpen(LocalDateTime.now()) && visible(portfolio);
    }

    private static boolean visible(PortfolioBrief portfolio) {
        return portfolio != null && portfolio.isPublic() && !portfolio.deleted();
    }

    private static String titleFor(PortfolioBrief portfolio) {
        return visible(portfolio) ? portfolio.title() : MyReviewItem.HIDDEN_TITLE;
    }

    private static String destination(PortfolioBrief portfolio) {
        return visible(portfolio) ? "/portfolios/" + portfolio.portfolioId() : REVIEWS_URL;
    }

    private static LocalDateTime openSince() {
        return LocalDateTime.now().minusDays(ReviewRequest.OPEN_DAYS);
    }
}
