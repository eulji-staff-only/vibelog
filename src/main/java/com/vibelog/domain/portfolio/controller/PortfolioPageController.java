package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.model.dto.ReviewView;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.portfolio.model.dto.MyPortfolioCard;
import com.vibelog.domain.portfolio.model.dto.PortfolioDetailView;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioPageView;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.service.PortfolioManageService;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import com.vibelog.domain.portfolio.model.service.PortfolioSubmitService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/*
 * [PortfolioPageController]
 * 역할 : 포트폴리오 영역의 화면을 연다. 과제 화면(API-PRT-01)·설정 파일 내려받기(API-PRT-03)·제출(API-PRT-04),
 *        내 포트폴리오(API-PRT-05)·수정(API-PRT-06)·삭제(API-PRT-07)·상세(API-PRT-09)·같은 코스 작품(API-PRT-10, P2)을 받는다.
 * 책임 : - 로그인 회원은 여기서만 꺼내고 Service 에는 memberId 만 넘긴다 (README "로그인 약속").
 *        - 수강하지 않은 코스면 코스 소개(/courses/{courseId})로 보낸다 (명세 실패 칸).
 *        - 제출 입력 오류(검증·금칙어·파일)는 같은 폼 다시 + 칸별 메시지(_template.md 7-1 ④). 이미 제출함은 과제 화면 + toast,
 *          잠김·미인증처럼 폼으로 고칠 수 없는 것은 오류 화면으로 보낸다 (CommunityPageController 와 같은 방식).
 *        - 수정은 U-17 수정 상태(edit=true)에서 보낸다. 입력 오류면 U-17 수정 상태 폼을 다시 그린다 (제출과 같은 방식).
 *          수정·삭제가 끝나면 내 포트폴리오 + toast. 남의 비공개 상세(PRIVATE_PORTFOLIO)·NOT_OWNER 는 오류 화면(E-02)이다.
 *        - 동료 리뷰(#281): U-20 받은 리뷰·리뷰 요청/마감 버튼, U-19 "리뷰 요청 중" 표시는 community 영역 CommunityReader 값을 붙인다.
 *          요청·마감·도움됨 처리는 community 영역 API 가 한다 (community.md "U-19·U-20 은 portfolio 화면").
 */
@Controller
@RequiredArgsConstructor
public class PortfolioPageController {

    private static final String VIEW = "portfolio/portfolio";

    private final PortfolioService portfolioService;
    private final PortfolioSubmitService portfolioSubmitService;
    private final PortfolioManageService portfolioManageService;
    private final CommunityReader communityReader;

    /* U-17 포트폴리오 (과제 → 평가 에이전트 자가 점검 → 제출). edit=true 면 제출한 내 포트폴리오 값으로 채운 수정 상태 */
    @GetMapping("/courses/{courseId}/portfolio")
    public String portfolio(@PathVariable Long courseId,
                            @RequestParam(defaultValue = "false") boolean edit,
                            @AuthenticationPrincipal LoginMember loginMember,
                            Model model) {
        Long memberId = loginMember.getMemberId();
        String view = pageView(courseId, memberId, model);
        PortfolioPageView page = (PortfolioPageView) model.getAttribute("page");
        if (edit && page != null && page.submittedPortfolioId() != null) {
            model.addAttribute("portfolioSubmitForm", portfolioManageService.getEditForm(memberId, page.submittedPortfolioId()));
            model.addAttribute("editPortfolioId", page.submittedPortfolioId());
        } else {
            model.addAttribute("portfolioSubmitForm", new PortfolioSubmitForm());
        }
        return view;
    }

    /* API-PRT-04 제출 → U-18 코스 완료 */
    @PostMapping("/courses/{courseId}/portfolio")
    public String submit(@PathVariable Long courseId,
                         @Valid @ModelAttribute PortfolioSubmitForm portfolioSubmitForm, BindingResult bindingResult,
                         @RequestParam(required = false) MultipartFile screenshot,
                         @AuthenticationPrincipal LoginMember loginMember,
                         Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (bindingResult.hasErrors()) {
            return pageView(courseId, memberId, model);
        }
        try {
            portfolioSubmitService.submit(memberId, courseId, portfolioSubmitForm, screenshot);
            return "redirect:/courses/" + courseId + "/complete";
        } catch (BusinessException e) {
            if (e.getErrorCode() == PortfolioErrorCode.ALREADY_SUBMITTED) {
                redirectAttributes.addFlashAttribute("toast", e.getMessage());
                return "redirect:/courses/" + courseId + "/portfolio";
            }
            rejectOrThrow(e, bindingResult);
            return pageView(courseId, memberId, model);
        }
    }

    /* API-PRT-06 수정 → 내 포트폴리오 + toast */
    @PostMapping("/portfolios/{portfolioId}/edit")
    public String edit(@PathVariable Long portfolioId,
                       @Valid @ModelAttribute PortfolioSubmitForm portfolioSubmitForm, BindingResult bindingResult,
                       @RequestParam(required = false) MultipartFile screenshot,
                       @AuthenticationPrincipal LoginMember loginMember,
                       Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (!bindingResult.hasErrors()) {
            try {
                portfolioSubmitService.edit(memberId, portfolioId, portfolioSubmitForm, screenshot);
                redirectAttributes.addFlashAttribute("toast", "포트폴리오를 수정했습니다");
                return "redirect:/my/portfolios";
            } catch (BusinessException e) {
                rejectOrThrow(e, bindingResult);
            }
        }
        model.addAttribute("editPortfolioId", portfolioId);
        return pageView(portfolioManageService.getOwnedCourseId(memberId, portfolioId), memberId, model);
    }

    /* API-PRT-07 삭제 (M-04 확인 후) → 내 포트폴리오 + toast */
    @PostMapping("/portfolios/{portfolioId}/delete")
    public String delete(@PathVariable Long portfolioId,
                         @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        portfolioManageService.delete(loginMember.getMemberId(), portfolioId);
        redirectAttributes.addFlashAttribute("toast", "포트폴리오를 삭제했습니다");
        return "redirect:/my/portfolios";
    }

    /*
     * 폼으로 고칠 수 있는 오류(금칙어·파일·에이전트 점검 결과 길이)는 같은 폼의 메시지로 바꾼다.
     * 잠김·미인증·NOT_OWNER 처럼 폼으로 고칠 수 없는 것은 그대로 던져 오류 화면으로 보낸다
     */
    private void rejectOrThrow(BusinessException e, BindingResult bindingResult) {
        if (e.getErrorCode() == PortfolioErrorCode.INVALID_INPUT) {
            // 제출·수정에서 INVALID_INPUT 은 에이전트 점검 결과가 저장 칸보다 길 때뿐이다
            bindingResult.rejectValue("agentResult", e.getErrorCode().toString(), "에이전트 점검 결과가 너무 깁니다. 줄여서 적어 주세요.");
            return;
        }
        if (e.getErrorCode() != CommonErrorCode.BANNED_WORD && e.getErrorCode() != CommonErrorCode.INVALID_FILE) {
            throw e;
        }
        bindingResult.reject(e.getErrorCode().toString(), e.getMessage());
    }

    /* 수강하지 않은 코스면 코스 소개로 */
    private String pageView(Long courseId, Long memberId, Model model) {
        return portfolioService.getPage(memberId, courseId)
                .map(page -> {
                    model.addAttribute("page", page);
                    return VIEW;
                })
                .orElse("redirect:/courses/" + courseId);
    }

    /*
     * API-PRT-03 설정 파일 내려받기. 과제가 열렸는지 확인한 뒤, 저장된 파일(/uploads/...)을 이 주소 그대로 응답한다.
     * 파일을 읽어 보내는 일은 FileStorage 가 등록한 /uploads/** 처리기에 맡기고(forward), 여기서는 "내려받기" 헤더만 붙인다.
     */
    @GetMapping("/courses/{courseId}/portfolio/agent-file")
    public String agentFile(@PathVariable Long courseId,
                            @AuthenticationPrincipal LoginMember loginMember,
                            HttpServletResponse response) {
        String fileUrl = portfolioService.getAgentFileUrl(loginMember.getMemberId(), courseId);
        String fileName = "agent-file." + StringUtils.getFilenameExtension(fileUrl);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString());
        return "forward:" + fileUrl;
    }

    /* U-19 내 포트폴리오 (API-PRT-05) */
    @GetMapping("/my/portfolios")
    public String myPortfolios(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        List<MyPortfolioCard> portfolios = portfolioManageService.getMyPortfolios(loginMember.getMemberId());
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("reviewOpenIds", communityReader.findPortfolioIdsWithOpenRequest(
                portfolios.stream().map(MyPortfolioCard::portfolioId).toList()));
        return "portfolio/my-portfolios";
    }

    /* U-35 같은 코스 작품 보기 (API-PRT-10). 포트폴리오를 내지 않았으면 과제 화면 + toast (③) */
    @GetMapping("/courses/{courseId}/portfolios")
    public String coursePortfolios(@PathVariable Long courseId, @RequestParam(defaultValue = "1") int page,
                                   @AuthenticationPrincipal LoginMember loginMember,
                                   Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("view", portfolioManageService.getCoursePortfolios(loginMember.getMemberId(), courseId, page));
            return "portfolio/course-portfolios";
        } catch (BusinessException e) {
            if (e.getErrorCode() != PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED) {
                throw e;
            }
            redirectAttributes.addFlashAttribute("toast", e.getMessage());
            return "redirect:/courses/" + courseId + "/portfolio";
        }
    }

    /* U-20 포트폴리오 상세 (API-PRT-09) */
    @GetMapping("/portfolios/{portfolioId}")
    public String detail(@PathVariable Long portfolioId,
                         @AuthenticationPrincipal LoginMember loginMember,
                         Model model) {
        Long memberId = loginMember.getMemberId();
        PortfolioDetailView portfolio = portfolioManageService.getDetail(memberId, portfolioId);
        List<ReviewView> reviews = communityReader.findReviewsOfPortfolio(portfolioId, memberId);
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("reviews", reviews);
        // 내가 쓴 리뷰의 수정 링크(/reviews?reviewRequestId=)에 쓸 요청 ID
        model.addAttribute("myReviewRequestIds", communityReader.findReviewRequestIdsOfReviews(
                reviews.stream().filter(ReviewView::mine).map(ReviewView::peerReviewId).toList()));
        if (portfolio.mine()) {
            model.addAttribute("openReviewRequestId", communityReader.findOpenReviewRequestId(portfolioId).orElse(null));
        }
        return "portfolio/detail";
    }
}
