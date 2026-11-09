package com.vibelog.domain.portfolio.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckResult;
import com.vibelog.domain.community.model.dto.ReviewView;
import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.portfolio.model.dto.CoursePortfolioCard;
import com.vibelog.domain.portfolio.model.dto.CoursePortfoliosView;
import com.vibelog.domain.portfolio.model.dto.MyPortfolioCard;
import com.vibelog.domain.portfolio.model.dto.PortfolioDetailView;
import com.vibelog.domain.portfolio.model.dto.PortfolioPageView;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.service.PortfolioManageService;
import com.vibelog.domain.portfolio.model.service.PortfolioService;
import com.vibelog.domain.portfolio.model.service.PortfolioTemplateService;
import com.vibelog.domain.portfolio.model.dto.TemplateItemView;
import com.vibelog.domain.portfolio.model.service.PortfolioSubmitService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 포트폴리오 과제 화면·설정 파일·스텝 체크 요청(API-PRT-01~03)이 로그인 회원의 memberId 만 Service 에 넘기고,
 * 결과에 맞는 화면·redirect·파일·JSON 으로 답하는지 확인한다 (DB 없이 도는 슬라이스 테스트).
 * 템플릿이 실제 데이터로 오류 없이 그려지는지도 여기서 잡는다.
 */
@WebMvcTest({PortfolioPageController.class, PortfolioGuideStepApiController.class, PortfolioVisibilityApiController.class})
class PortfolioPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PortfolioService portfolioService;

    @MockitoBean
    PortfolioSubmitService portfolioSubmitService;

    @MockitoBean
    PortfolioManageService portfolioManageService;

    @MockitoBean
    CommunityReader communityReader;

    @MockitoBean
    PortfolioTemplateService portfolioTemplateService;

    /* U-34 항목은 템플릿 관리(A-12) 값으로 그린다. 따로 정하지 않은 테스트는 기본 5개 */
    @BeforeEach
    void templateDefaults() {
        given(portfolioTemplateService.getItems()).willReturn(TemplateItemView.defaults());
    }

    private RequestPostProcessor login() {
        LoginMember loginMember = mock(LoginMember.class);
        given(loginMember.getMemberId()).willReturn(1L);
        return authentication(new UsernamePasswordAuthenticationToken(loginMember, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
    }

    private PortfolioPageView openPage(Long submittedPortfolioId, boolean emailVerified) {
        return openPage(submittedPortfolioId, emailVerified, List.of());
    }

    private PortfolioPageView openPage(Long submittedPortfolioId, boolean emailVerified, List<PortfolioPageView.Faq> faqs) {
        return new PortfolioPageView(3L, "바이브 코스", true, null, "할 일 앱 만들기", "목표", "세팅 방법 안내", true,
                List.of(new PortfolioPageView.Step(11L, "첫 스텝", true), new PortfolioPageView.Step(12L, "둘째 스텝", false)),
                1, submittedPortfolioId, emailVerified, faqs);
    }

    @Test
    void 열린_과제는_과제_스텝_안내와_제출_양식을_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("할 일 앱 만들기")))
                .andExpect(content().string(Matchers.containsString("둘째 스텝")))
                .andExpect(content().string(Matchers.containsString("/api/guide-steps/11/check")))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolio/agent-file")))
                .andExpect(content().string(Matchers.containsString("세팅 방법 안내")))
                .andExpect(content().string(Matchers.containsString("data-pf-form")))
                .andExpect(content().string(Matchers.containsString("vl-header")))
                .andExpect(content().string(Matchers.containsString("vl-footer")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("아직 열리지 않았습니다"))));
    }

    @Test
    void FAQ가_있으면_4번_영역에_질문과_답을_그리고_없으면_그리지_않는다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true,
                List.of(new PortfolioPageView.Faq("커밋을 안 했어요", "git stash 로 치워 두세요")))));
        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("커밋을 안 했어요")))
                .andExpect(content().string(Matchers.containsString("git stash 로 치워 두세요")));

        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));
        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.not(Matchers.containsString("id=\"pf-faq-h\""))));
    }

    @Test
    void 이메일_미인증이면_제출_양식_대신_인증_안내를_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, false)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("이메일 인증 후 포트폴리오를 제출할 수 있습니다.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-pf-form"))));
    }

    @Test
    void 이미_제출했으면_내_포트폴리오_링크를_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(21L, true)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("/portfolios/21")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-pf-form"))))
                // 제출이 끝났으니 보관해 둔 작성 내용을 지운다 (FR-PRT-010)
                .andExpect(content().string(Matchers.containsString("data-draft-clear=\"vibelog.portfolio-draft.1.3\"")));
    }

    @Test
    void 제출_양식에_회원_코스별_임시_저장_키를_넘긴다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(content().string(Matchers.containsString("data-draft-key=\"vibelog.portfolio-draft.1.3\"")))
                .andExpect(content().string(Matchers.containsString("data-draft-notice hidden")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-draft-clear"))));
    }

    @Test
    void 잠긴_과제는_리포트_안내만_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(PortfolioPageView.locked(3L, "바이브 코스", 7L)));

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("아직 열리지 않았습니다")))
                .andExpect(content().string(Matchers.containsString("/lessons/7/exam/report")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("포트폴리오 제출"))));
    }

    @Test
    void 수강하지_않은_코스면_코스_소개로_보낸다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.empty());

        mockMvc.perform(get("/courses/3/portfolio").with(login()))
                .andExpect(redirectedUrl("/courses/3"));
    }

    @Test
    void 설정_파일은_내려받기_헤더를_붙여_저장된_파일로_넘긴다() throws Exception {
        given(portfolioService.getAgentFileUrl(1L, 3L)).willReturn("/uploads/abc.md");

        mockMvc.perform(get("/courses/3/portfolio/agent-file").with(login()))
                .andExpect(forwardedUrl("/uploads/abc.md"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"agent-file.md\""));
    }

    @Test
    void 잠긴_과제의_설정_파일은_내려받을_수_없다() throws Exception {
        given(portfolioService.getAgentFileUrl(1L, 3L)).willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED));

        mockMvc.perform(get("/courses/3/portfolio/agent-file").with(login()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"));
    }

    @Test
    void 스텝_체크는_JSON으로_완료_수를_돌려준다() throws Exception {
        given(portfolioService.check(1L, 11L, true)).willReturn(new GuideStepCheckResult(2, 3));

        mockMvc.perform(post("/api/guide-steps/11/check").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedCount").value(2))
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    void 잠긴_과제의_스텝_체크는_JSON_오류로_답한다() throws Exception {
        given(portfolioService.check(1L, 11L, true)).willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED));

        mockMvc.perform(post("/api/guide-steps/11/check").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checked\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PORTFOLIO_LOCKED"));
    }

    @Test
    void 수정을_마치고_오면_작성_내용_보관을_지우는_표시를_그린다() throws Exception {
        mockMvc.perform(get("/my/portfolios").with(login()).flashAttr("clearDraft", "vibelog.portfolio-draft.1.3.edit"))
                .andExpect(content().string(Matchers.containsString("data-draft-clear=\"vibelog.portfolio-draft.1.3.edit\"")));
    }

    @Test
    void 같은_코스_작품은_카드와_상세_링크_페이지_번호를_그린다() throws Exception {
        given(portfolioManageService.getCoursePortfolios(1L, 3L, 2)).willReturn(new CoursePortfoliosView(3L, "바이브 코스",
                new org.springframework.data.domain.PageImpl<>(List.of(new CoursePortfolioCard(7L, "할 일 앱", "요약", "작성자",
                        LocalDateTime.of(2026, 11, 9, 10, 0))), org.springframework.data.domain.PageRequest.of(1, 20), 21)));

        mockMvc.perform(get("/courses/3/portfolios").param("page", "2").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/course-portfolios"))
                .andExpect(content().string(Matchers.containsString("바이브 코스")))
                .andExpect(content().string(Matchers.containsString("/portfolios/7")))
                .andExpect(content().string(Matchers.containsString("작성자 · 제출일 2026.11.09")))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolios?page=2")))
                .andExpect(content().string(Matchers.containsString("aria-current=\"page\">2</a>")));
    }

    @Test
    void 포트폴리오를_내지_않았으면_과제_화면으로_보내고_안내한다() throws Exception {
        given(portfolioManageService.getCoursePortfolios(1L, 3L, 1))
                .willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED));

        mockMvc.perform(get("/courses/3/portfolios").with(login()))
                .andExpect(redirectedUrl("/courses/3/portfolio"))
                .andExpect(flash().attribute("toast", PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED.getMessage()));
    }

    @Test
    void 내_포트폴리오는_카드와_공개_전환_수정_삭제를_그린다() throws Exception {
        given(portfolioManageService.getMyPortfolios(1L)).willReturn(List.of(
                new MyPortfolioCard(7L, 3L, "바이브 코스", "할 일 앱", "한 줄 요약", false, LocalDateTime.of(2026, 11, 9, 10, 0))));

        mockMvc.perform(get("/my/portfolios").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/my-portfolios"))
                .andExpect(content().string(Matchers.containsString("할 일 앱")))
                .andExpect(content().string(Matchers.containsString("제출일 2026.11.09")))
                .andExpect(content().string(Matchers.containsString("공개로 전환")))
                .andExpect(content().string(Matchers.containsString("/api/portfolios/7/visibility")))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolio?edit=true")))
                .andExpect(content().string(Matchers.containsString("/portfolios/7/delete")))
                .andExpect(content().string(Matchers.containsString("포트폴리오를 삭제할까요?")));
    }

    @Test
    void 포트폴리오가_없으면_빈_상태를_그린다() throws Exception {
        given(portfolioManageService.getMyPortfolios(1L)).willReturn(List.of());

        mockMvc.perform(get("/my/portfolios").with(login()))
                .andExpect(content().string(Matchers.containsString("제출한 포트폴리오가 없습니다.")));
    }

    private PortfolioDetailView detailView(boolean isPublic, boolean mine) {
        return new PortfolioDetailView(7L, "할 일 앱", "https://github.com/me/todo", "요약", "회고", "Claude Code", null, null,
                3L, "바이브 코스", "작성자", LocalDateTime.of(2026, 11, 9, 10, 0), isPublic, mine, false);
    }

    private ReviewView review(boolean helpful, boolean mine) {
        return new ReviewView(11L, 2L, "리뷰어", LocalDateTime.of(2026, 11, 9, 11, 0), helpful, mine,
                List.of(new ReviewView.Comment("요청 범위", "범위가 분명해요"), new ReviewView.Comment(null, "전체 의견")), false, false);
    }

    @Test
    void 영구_정지_회원의_리뷰는_숨김_안내만_그린다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(detailView(true, true));
        given(communityReader.findReviewsOfPortfolio(7L, 1L)).willReturn(List.of(
                new ReviewView(11L, 2L, null, LocalDateTime.of(2026, 11, 9, 11, 0), false, false, List.of(), true, false)));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(content().string(Matchers.containsString("운영 정책에 따라 숨겨진 콘텐츠입니다")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-helpful-url"))));
    }

    @Test
    void 내_포트폴리오_카드에_리뷰_요청_중을_표시한다() throws Exception {
        given(portfolioManageService.getMyPortfolios(1L)).willReturn(List.of(
                new MyPortfolioCard(7L, 3L, "바이브 코스", "할 일 앱", "요약", true, LocalDateTime.of(2026, 11, 9, 10, 0))));
        given(communityReader.findPortfolioIdsWithOpenRequest(List.of(7L))).willReturn(Set.of(7L));

        mockMvc.perform(get("/my/portfolios").with(login()))
                .andExpect(content().string(Matchers.containsString("리뷰 요청 중")));
    }

    @Test
    void 내_공개_작품에_열린_요청이_없으면_리뷰_요청_버튼과_확인_모달을_그린다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(detailView(true, true));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(content().string(Matchers.containsString("data-ui-modal-open=\"review-request-modal\"")))
                .andExpect(content().string(Matchers.containsString("리뷰를 요청할까요?")))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/7/review-requests\"")))
                .andExpect(content().string(Matchers.containsString("아직 받은 리뷰가 없습니다.")));
    }

    @Test
    void 열린_요청이_있으면_리뷰_마감_버튼을_그린다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(detailView(true, true));
        given(communityReader.findOpenReviewRequestId(7L)).willReturn(Optional.of(5L));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(content().string(Matchers.containsString("action=\"/review-requests/5/close\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("리뷰를 요청할까요?"))));
    }

    @Test
    void 내_작품의_받은_리뷰에는_도움됨_버튼이_있다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(detailView(true, true));
        given(communityReader.findReviewsOfPortfolio(7L, 1L)).willReturn(List.of(review(true, false)));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(content().string(Matchers.containsString("받은 리뷰 1개")))
                .andExpect(content().string(Matchers.containsString("요청 범위")))
                .andExpect(content().string(Matchers.containsString("범위가 분명해요")))
                .andExpect(content().string(Matchers.containsString("data-helpful-url=\"/api/reviews/11/helpful\"")))
                .andExpect(content().string(Matchers.containsString("aria-pressed=\"true\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("내 리뷰 수정"))));
    }

    @Test
    void 남의_작품에서는_도움됨_버튼_대신_표시만_있고_내_리뷰면_수정_링크가_있다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(detailView(true, false));
        given(communityReader.findReviewsOfPortfolio(7L, 1L)).willReturn(List.of(review(true, true)));
        given(communityReader.findReviewRequestIdsOfReviews(List.of(11L))).willReturn(Map.of(11L, 3L));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-helpful-url"))))
                .andExpect(content().string(Matchers.containsString("is-success\">도움됨")))
                .andExpect(content().string(Matchers.containsString("/reviews?reviewRequestId=3")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("리뷰를 요청할까요?"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("리뷰 마감"))));
    }

    @Test
    void 내_비공개_상세는_안내와_수정을_그린다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willReturn(new PortfolioDetailView(7L, "할 일 앱",
                "https://github.com/me/todo", "요약", "회고", "Claude Code", null, null, 3L, "바이브 코스", "나",
                LocalDateTime.of(2026, 11, 9, 10, 0), false, true, false));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/detail"))
                .andExpect(content().string(Matchers.containsString("비공개로 제출한 포트폴리오입니다.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("review-request-modal"))))
                .andExpect(content().string(Matchers.containsString("/courses/3/portfolio?edit=true")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("에이전트 점검 결과"))));
    }

    @Test
    void 남의_비공개_상세는_오류_화면으로_보낸다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willThrow(new BusinessException(PortfolioErrorCode.PRIVATE_PORTFOLIO));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"))
                .andExpect(content().string(Matchers.containsString("비공개된 작품입니다.")));
    }

    @Test
    void 수정_상태는_내_포트폴리오_값으로_채우고_수정_주소로_보낸다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(7L, true)));
        PortfolioSubmitForm saved = new PortfolioSubmitForm();
        saved.setTitle("저장된 제목");
        saved.setIsPublic(false);
        given(portfolioManageService.getEditForm(1L, 7L)).willReturn(saved);

        mockMvc.perform(get("/courses/3/portfolio").param("edit", "true").with(login()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("value=\"저장된 제목\"")))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/7/edit\"")))
                .andExpect(content().string(Matchers.containsString("수정하기")))
                .andExpect(content().string(Matchers.containsString("aria-pressed=\"true\">비공개")))
                // 수정 중인 내용은 새 제출과 다른 키에 보관한다 (새 제출 키를 지울 때 함께 지워지지 않게)
                .andExpect(content().string(Matchers.containsString("data-draft-key=\"vibelog.portfolio-draft.1.3.edit\"")));
    }

    /* ---------- #199 개인 작품 U-34 ---------- */

    @Test
    void 개인_작품_추가_화면은_고정_항목_5개와_공개_여부_양식을_그린다() throws Exception {
        given(portfolioManageService.isEmailVerified(1L)).willReturn(true);

        mockMvc.perform(get("/my/portfolios/new").param("template", "basic").with(login()))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/personal"))
                .andExpect(content().string(Matchers.containsString("action=\"/my/portfolios\"")))
                .andExpect(content().string(Matchers.containsString(">설명 <em>*</em>")))
                .andExpect(content().string(Matchers.containsString("등록하기")))
                .andExpect(content().string(Matchers.containsString("data-draft-key=\"vibelog.portfolio-draft.1.personal\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"screenshot\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"agentResult\""))));
    }

    @Test
    void 개인_작품_화면은_운영자가_정한_항목_이름_안내_문구_순서로_그린다() throws Exception {
        given(portfolioManageService.isEmailVerified(1L)).willReturn(true);
        given(portfolioTemplateService.getItems()).willReturn(List.of(
                new TemplateItemView("RETROSPECTIVE", "돌아보기", "무엇을 배웠나요?"),
                new TemplateItemView("TITLE", "작품 이름", null),
                new TemplateItemView("LINK", "링크", null),
                new TemplateItemView("DESCRIPTION", "한 줄 소개", null),
                new TemplateItemView("AI_TOOLS", "AI 도구", null)));

        String html = mockMvc.perform(get("/my/portfolios/new").with(login()))
                .andExpect(content().string(Matchers.containsString("돌아보기 <em>*</em>")))
                .andExpect(content().string(Matchers.containsString("placeholder=\"무엇을 배웠나요?\"")))
                .andReturn().getResponse().getContentAsString();

        // 운영자가 정한 순서: 회고 칸(retrospective)이 제목 칸(title)보다 먼저 나온다. 저장 칸 이름은 그대로다
        org.assertj.core.api.Assertions.assertThat(html.indexOf("name=\"retrospective\"")).isLessThan(html.indexOf("name=\"title\""));
        org.assertj.core.api.Assertions.assertThat(html).contains("한 줄 소개 <em>*</em>").contains("name=\"summary\"");
    }

    @Test
    void 이메일_미인증이면_개인_작품_양식_대신_인증_안내를_그린다() throws Exception {
        mockMvc.perform(get("/my/portfolios/new").with(login()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("이메일 인증 후 개인 작품을 등록할 수 있습니다.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("data-submit-form"))));
    }

    @Test
    void 개인_작품_수정_상태는_저장된_값으로_채우고_수정_주소로_보낸다() throws Exception {
        given(portfolioManageService.isEmailVerified(1L)).willReturn(true);
        PortfolioSubmitForm saved = new PortfolioSubmitForm();
        saved.setTitle("내 개인 작품");
        saved.setIsPublic(true);
        given(portfolioManageService.getPersonalEditForm(1L, 9L)).willReturn(saved);

        mockMvc.perform(get("/my/portfolios/new").param("edit", "9").with(login()))
                .andExpect(content().string(Matchers.containsString("value=\"내 개인 작품\"")))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/9/edit\"")))
                .andExpect(content().string(Matchers.containsString("수정하기")))
                .andExpect(content().string(Matchers.containsString("data-draft-key=\"vibelog.portfolio-draft.1.personal.9\"")));
    }

    @Test
    void 개인_작품을_등록하면_내_포트폴리오로_보내고_안내한다() throws Exception {
        mockMvc.perform(post("/my/portfolios").with(login()).with(csrf())
                        .param("title", "개인 작품").param("resultUrl", "https://github.com/me/app").param("summary", "설명")
                        .param("retrospective", RETRO).param("aiTools", "Claude Code").param("isPublic", "true"))
                .andExpect(redirectedUrl("/my/portfolios"))
                .andExpect(flash().attribute("toast", "개인 작품을 등록했습니다"))
                .andExpect(flash().attribute("clearDraft", "vibelog.portfolio-draft.1.personal"));

        verify(portfolioSubmitService).submitPersonal(eq(1L), any(PortfolioSubmitForm.class));
    }

    @Test
    void 개인_작품_입력이_틀리면_같은_양식을_다시_그린다() throws Exception {
        given(portfolioManageService.isEmailVerified(1L)).willReturn(true);

        mockMvc.perform(post("/my/portfolios").with(login()).with(csrf())
                        .param("title", "개인 작품").param("resultUrl", "주소 아님").param("summary", "설명")
                        .param("retrospective", "짧음").param("aiTools", "Claude Code").param("isPublic", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/personal"))
                .andExpect(content().string(Matchers.containsString("올바른 URL을 입력하세요")))
                .andExpect(content().string(Matchers.containsString("회고는 100자 이상 2,000자 이하로 입력해 주세요.")));

        verify(portfolioSubmitService, never()).submitPersonal(any(), any());
    }

    @Test
    void 개인_작품_수정_입력이_틀리면_U34_수정_상태를_다시_그린다() throws Exception {
        given(portfolioManageService.getOwnedCourseId(1L, 7L)).willReturn(null);
        given(portfolioManageService.isEmailVerified(1L)).willReturn(true);

        mockMvc.perform(editRequest("너무 짧은 회고"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/personal"))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/7/edit\"")));
    }

    @Test
    void 개인_작품을_수정하면_개인_작품_수정_키를_지운다() throws Exception {
        given(portfolioManageService.getOwnedCourseId(1L, 7L)).willReturn(null);

        mockMvc.perform(editRequest(RETRO))
                .andExpect(redirectedUrl("/my/portfolios"))
                .andExpect(flash().attribute("clearDraft", "vibelog.portfolio-draft.1.personal.7"));
    }

    @Test
    void 내_포트폴리오에서_개인_작품은_U34로_수정하고_삭제_안내를_구분한다() throws Exception {
        given(portfolioManageService.getMyPortfolios(1L)).willReturn(List.of(
                new MyPortfolioCard(9L, null, null, "개인 작품", "설명", true, LocalDateTime.of(2026, 11, 9, 10, 0))));

        mockMvc.perform(get("/my/portfolios").with(login()))
                .andExpect(content().string(Matchers.containsString("개인 작품 추가")))
                .andExpect(content().string(Matchers.containsString("/my/portfolios/new?edit=9")))
                .andExpect(content().string(Matchers.containsString("data-course=\"false\"")))
                .andExpect(content().string(Matchers.containsString("data-delete-warn")));
    }

    @Test
    void 내_개인_작품_상세의_수정은_U34로_간다() throws Exception {
        given(portfolioManageService.getDetail(1L, 9L)).willReturn(new PortfolioDetailView(9L, "개인 작품", "https://github.com/me/app",
                "설명", "회고", "Claude Code", null, null, null, null, "나", LocalDateTime.of(2026, 11, 9, 10, 0), true, true, false));

        mockMvc.perform(get("/portfolios/9").with(login()))
                .andExpect(content().string(Matchers.containsString("/my/portfolios/new?edit=9")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("edit=true"))));
    }

    @Test
    void 수정하면_내_포트폴리오로_보내고_안내한다() throws Exception {
        given(portfolioManageService.getOwnedCourseId(1L, 7L)).willReturn(3L);

        mockMvc.perform(editRequest(RETRO))
                .andExpect(redirectedUrl("/my/portfolios"))
                .andExpect(flash().attribute("toast", "포트폴리오를 수정했습니다"))
                // 수정이 끝났으니 내 포트폴리오 화면이 그 코스의 작성 내용 보관을 지운다 (FR-PRT-010)
                .andExpect(flash().attribute("clearDraft", "vibelog.portfolio-draft.1.3.edit"));

        verify(portfolioSubmitService).edit(eq(1L), eq(7L), any(PortfolioSubmitForm.class), isNull());
    }

    @Test
    void 수정_입력이_틀리면_수정_상태_폼을_다시_그린다() throws Exception {
        given(portfolioManageService.getOwnedCourseId(1L, 7L)).willReturn(3L);
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(7L, true)));

        mockMvc.perform(editRequest("너무 짧은 회고"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("회고는 100자 이상 2,000자 이하로 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/7/edit\"")));

        verify(portfolioSubmitService, never()).edit(any(), any(), any(), any());
    }

    @Test
    void 출품_중인_작품을_수정하면_수정_상태_폼에_안내를_보여_준다() throws Exception {
        given(portfolioManageService.getOwnedCourseId(1L, 7L)).willReturn(3L);
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(7L, true)));
        willThrow(new BusinessException(PortfolioErrorCode.CONTEST_ENTERED))
                .given(portfolioSubmitService).edit(eq(1L), eq(7L), any(), any());

        mockMvc.perform(editRequest(RETRO))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("콘테스트에 출품한 작품은 수정할 수 없습니다.")))
                .andExpect(content().string(Matchers.containsString("action=\"/portfolios/7/edit\"")));
    }

    @Test
    void 영구_정지_회원의_포트폴리오_상세는_숨김_안내_오류_화면으로_보낸다() throws Exception {
        given(portfolioManageService.getDetail(1L, 7L)).willThrow(new BusinessException(PortfolioErrorCode.HIDDEN_CONTENT));

        mockMvc.perform(get("/portfolios/7").with(login()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"))
                .andExpect(content().string(Matchers.containsString("운영 정책에 따라 숨겨진 콘텐츠입니다.")));
    }

    @Test
    void 남의_포트폴리오_수정은_오류_화면으로_보낸다() throws Exception {
        willThrow(new BusinessException(CommonErrorCode.NOT_OWNER))
                .given(portfolioSubmitService).edit(eq(1L), eq(7L), any(), any());

        mockMvc.perform(editRequest(RETRO))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"));
    }

    @Test
    void 삭제하면_내_포트폴리오로_보내고_안내한다() throws Exception {
        mockMvc.perform(post("/portfolios/7/delete").with(login()).with(csrf()))
                .andExpect(redirectedUrl("/my/portfolios"))
                .andExpect(flash().attribute("toast", "포트폴리오를 삭제했습니다"));

        verify(portfolioManageService).delete(1L, 7L);
    }

    @Test
    void 공개_전환은_JSON으로_바뀐_값을_돌려준다() throws Exception {
        given(portfolioManageService.changeVisibility(1L, 7L, true)).willReturn(true);

        mockMvc.perform(post("/api/portfolios/7/visibility").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isPublic\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void 남의_포트폴리오_공개_전환은_JSON_오류로_답한다() throws Exception {
        given(portfolioManageService.changeVisibility(1L, 7L, false))
                .willThrow(new BusinessException(CommonErrorCode.NOT_OWNER));

        mockMvc.perform(post("/api/portfolios/7/visibility").with(login()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isPublic\": false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder editRequest(String retrospective) {
        return multipart("/portfolios/7/edit").with(login()).with(csrf())
                .param("title", "할 일 앱").param("resultUrl", "https://github.com/me/todo").param("summary", "요약")
                .param("retrospective", retrospective).param("aiTools", "Claude Code").param("isPublic", "true");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submitRequest(String retrospective, String isPublic) {
        return multipart("/courses/3/portfolio").with(login()).with(csrf())
                .param("title", "할 일 앱").param("resultUrl", "https://github.com/me/todo").param("summary", "요약")
                .param("retrospective", retrospective).param("aiTools", "Claude Code").param("isPublic", isPublic);
    }

    private static final String RETRO = "배운 점을 적습니다. ".repeat(12);

    @Test
    void 제출하면_코스_완료_화면으로_간다() throws Exception {
        mockMvc.perform(submitRequest(RETRO, "true"))
                .andExpect(redirectedUrl("/courses/3/complete"));

        verify(portfolioSubmitService).submit(eq(1L), eq(3L), any(PortfolioSubmitForm.class), isNull());
    }

    @Test
    void 입력이_틀리면_같은_폼을_보낸_값과_칸별_메시지로_다시_그린다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));

        mockMvc.perform(submitRequest("너무 짧은 회고", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/portfolio"))
                .andExpect(content().string(Matchers.containsString("회고는 100자 이상 2,000자 이하로 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString("공개 여부를 골라 주세요.")))
                .andExpect(content().string(Matchers.containsString("value=\"할 일 앱\"")))
                .andExpect(content().string(Matchers.containsString("너무 짧은 회고")));

        verify(portfolioSubmitService, never()).submit(any(), any(), any(), any());
    }

    @Test
    void 금칙어면_같은_폼_위에_메시지를_보여_준다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));
        willThrow(new BusinessException(CommonErrorCode.BANNED_WORD))
                .given(portfolioSubmitService).submit(eq(1L), eq(3L), any(), any());

        mockMvc.perform(submitRequest(RETRO, "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(CommonErrorCode.BANNED_WORD.getMessage())))
                .andExpect(content().string(Matchers.containsString("aria-pressed=\"true\">비공개")));
    }

    @Test
    void 에이전트_점검_결과가_너무_길면_그_칸에_메시지를_보여_준다() throws Exception {
        given(portfolioService.getPage(1L, 3L)).willReturn(Optional.of(openPage(null, true)));
        willThrow(new BusinessException(PortfolioErrorCode.INVALID_INPUT))
                .given(portfolioSubmitService).submit(eq(1L), eq(3L), any(), any());

        mockMvc.perform(submitRequest(RETRO, "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("에이전트 점검 결과가 너무 깁니다.")));
    }

    @Test
    void 이미_제출했으면_과제_화면으로_보내고_안내한다() throws Exception {
        willThrow(new BusinessException(PortfolioErrorCode.ALREADY_SUBMITTED))
                .given(portfolioSubmitService).submit(eq(1L), eq(3L), any(), any());

        mockMvc.perform(submitRequest(RETRO, "true"))
                .andExpect(redirectedUrl("/courses/3/portfolio"))
                .andExpect(flash().attribute("toast", PortfolioErrorCode.ALREADY_SUBMITTED.getMessage()));
    }

    @Test
    void 과제가_잠겼으면_오류_화면으로_보낸다() throws Exception {
        willThrow(new BusinessException(PortfolioErrorCode.PORTFOLIO_LOCKED))
                .given(portfolioSubmitService).submit(eq(1L), eq(3L), any(), any());

        mockMvc.perform(submitRequest(RETRO, "true"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"));
    }
}
