package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dto.ReviewCommentInput;
import com.vibelog.domain.community.model.dto.ReviewForm;
import com.vibelog.domain.community.model.dto.ReviewFormView;
import com.vibelog.domain.community.model.service.PeerReviewService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [CommunityReviewController]
 * 역할 : 동료 리뷰 화면과 요청·마감·쓰기·수정·삭제 (U-31, API-REV-01~05·07, #182·#183).
 * 책임 : - U-31 은 위에 내가 쓴 리뷰, 가운데 리뷰 쓰기·수정 폼(reviewRequestId 가 있을 때), 아래에 리뷰를 기다리는 작품이다.
 *        - 요청·마감 실패는 작품 상세(U-20)로 돌아가 toast (방식 ③). 폼을 열 수 없으면(없음·마감·내 작품) /reviews + toast.
 *        - 리뷰 입력 오류(칸 검사, 기준 묶음, 금칙어, 미인증)는 같은 폼에 입력값과 메시지를 다시 보여 준다 (방식 ④).
 *        - 작품 상세는 portfolio 화면(U-20)이라, 리뷰를 끝내면 그 주소로 보낸다 (비공개·삭제면 /reviews).
 */
@Controller
@RequiredArgsConstructor
public class CommunityReviewController {

    private static final String VIEW = "community/reviews";
    /* 폼을 다시 그릴 수 없어 목록으로 보내는 실패 */
    private static final Set<ErrorCode> LEAVE_FORM = Set.of(CommunityErrorCode.REVIEW_REQUEST_NOT_FOUND,
            CommunityErrorCode.REVIEW_REQUEST_CLOSED, CommunityErrorCode.SELF_REVIEW);

    private final PeerReviewService peerReviewService;

    /* API-REV-02 */
    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "1") int myPage,
                          @RequestParam(required = false) Long reviewRequestId, @AuthenticationPrincipal LoginMember loginMember,
                          Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        ReviewFormView form = null;
        if (reviewRequestId != null) {
            try {
                form = peerReviewService.getForm(memberId, reviewRequestId);
            } catch (BusinessException e) {
                return backToList(e, redirectAttributes);
            }
            model.addAttribute("reviewForm", prefilled(form));
        }
        return view(memberId, page, myPage, form, model);
    }

    /* API-REV-01 (U-19·U-20 버튼, M-08 확인 뒤) */
    @PostMapping("/portfolios/{portfolioId}/review-requests")
    public String request(@PathVariable Long portfolioId, @AuthenticationPrincipal LoginMember loginMember,
                          RedirectAttributes redirectAttributes) {
        try {
            peerReviewService.request(loginMember.getMemberId(), portfolioId);
            redirectAttributes.addFlashAttribute("toast", "리뷰를 요청했습니다");
        } catch (BusinessException e) {
            if (e.getErrorCode() == CommunityErrorCode.PORTFOLIO_NOT_FOUND) {
                throw e;
            }
            redirectAttributes.addFlashAttribute("toast", e.getMessage());
        }
        return "redirect:/portfolios/" + portfolioId;
    }

    /* API-REV-07 (U-20 내 작품) */
    @PostMapping("/review-requests/{reviewRequestId}/close")
    public String close(@PathVariable Long reviewRequestId, @AuthenticationPrincipal LoginMember loginMember,
                        RedirectAttributes redirectAttributes) {
        Long portfolioId = peerReviewService.getPortfolioIdOfRequest(reviewRequestId);
        try {
            peerReviewService.close(loginMember.getMemberId(), reviewRequestId);
            redirectAttributes.addFlashAttribute("toast", "리뷰 요청을 마감했습니다");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("toast", e.getMessage());
        }
        return "redirect:/portfolios/" + portfolioId;
    }

    /* API-REV-03 */
    @PostMapping("/review-requests/{reviewRequestId}/reviews")
    public String write(@PathVariable Long reviewRequestId, @Valid @ModelAttribute ReviewForm reviewForm, BindingResult bindingResult,
                        @AuthenticationPrincipal LoginMember loginMember, Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (!bindingResult.hasErrors()) {
            try {
                String destination = peerReviewService.write(memberId, reviewRequestId, reviewForm.getComments());
                redirectAttributes.addFlashAttribute("toast", "리뷰를 남겼습니다");
                return "redirect:" + destination;
            } catch (BusinessException e) {
                if (e.getErrorCode() == CommunityErrorCode.ALREADY_REVIEWED) {
                    redirectAttributes.addFlashAttribute("toast", e.getMessage());
                    return "redirect:/reviews?reviewRequestId=" + reviewRequestId;
                }
                if (LEAVE_FORM.contains(e.getErrorCode())) {
                    return backToList(e, redirectAttributes);
                }
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        return formAgain(memberId, reviewRequestId, model, redirectAttributes);
    }

    /* API-REV-04. 남의 리뷰는 NOT_OWNER → 오류 화면 */
    @PostMapping("/peer-reviews/{peerReviewId}/edit")
    public String edit(@PathVariable Long peerReviewId, @Valid @ModelAttribute ReviewForm reviewForm, BindingResult bindingResult,
                       @AuthenticationPrincipal LoginMember loginMember, Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        Long reviewRequestId = peerReviewService.getReviewRequestIdOf(memberId, peerReviewId);
        if (!bindingResult.hasErrors()) {
            try {
                String destination = peerReviewService.edit(memberId, peerReviewId, reviewForm.getComments());
                redirectAttributes.addFlashAttribute("toast", "리뷰를 수정했습니다");
                return "redirect:" + destination;
            } catch (BusinessException e) {
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        return formAgain(memberId, reviewRequestId, model, redirectAttributes);
    }

    /* API-REV-05. 남의 리뷰는 NOT_OWNER → 오류 화면 */
    @PostMapping("/peer-reviews/{peerReviewId}/delete")
    public String delete(@PathVariable Long peerReviewId, @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        String destination = peerReviewService.delete(loginMember.getMemberId(), peerReviewId);
        redirectAttributes.addFlashAttribute("toast", "리뷰를 삭제했습니다");
        return "redirect:" + destination;
    }

    /* 입력값(reviewForm)은 모델에 그대로 두고, 칸 이름(기준)만 다시 읽어 폼을 그린다 */
    private String formAgain(Long memberId, Long reviewRequestId, Model model, RedirectAttributes redirectAttributes) {
        try {
            return view(memberId, 1, 1, peerReviewService.getForm(memberId, reviewRequestId), model);
        } catch (BusinessException e) {
            return backToList(e, redirectAttributes);
        }
    }

    private String backToList(BusinessException e, RedirectAttributes redirectAttributes) {
        if (!LEAVE_FORM.contains(e.getErrorCode())) {
            throw e;
        }
        redirectAttributes.addFlashAttribute("toast", e.getMessage());
        return "redirect:/reviews";
    }

    private static ReviewForm prefilled(ReviewFormView form) {
        ReviewForm reviewForm = new ReviewForm();
        reviewForm.setComments(form.fields().stream()
                .map(field -> new ReviewCommentInput(field.reviewCriterionId(), field.content()))
                .collect(Collectors.toCollection(ArrayList::new)));
        return reviewForm;
    }

    private String view(Long memberId, int page, int myPage, ReviewFormView form, Model model) {
        model.addAttribute("requests", peerReviewService.getOpenRequests(page));
        model.addAttribute("myReviews", peerReviewService.getMyReviews(memberId, myPage));
        model.addAttribute("form", form);
        model.addAttribute("myPage", myPage);
        return VIEW;
    }
}
