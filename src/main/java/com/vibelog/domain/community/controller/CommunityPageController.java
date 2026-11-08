package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dto.PostWriteRequest;
import com.vibelog.domain.community.model.entity.PostCategory;
import com.vibelog.domain.community.model.service.PostService;
import com.vibelog.domain.community.model.service.PostWriteService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
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

import java.util.List;

/*
 * [CommunityPageController]
 * 역할 : 커뮤니티 화면 요청을 받는다.
 * 책임 : - 게시판 목록(U-21, API-CMT-01)·게시글 상세(U-22, API-CMT-02)를 보여 준다. 비회원도 연다 (PL-USR-008).
 *        - 글쓰기·수정(U-23, API-CMT-03~06)과 삭제(API-CMT-07)는 회원만 (SecurityConfig). 작성자·인증 검사는 PostWriteService 가 한다.
 *        - 글쓰기와 수정은 같은 화면이고, 수정이면 editing 으로 주소·제목·버튼 문구를 바꾼다.
 *        - 입력 오류는 같은 폼에 입력값과 칸별 메시지로 다시 보여 준다 (방식 ④). 없는 글·남의 글은 오류 화면으로 간다 (방식 ①).
 */
@Controller
@RequiredArgsConstructor
public class CommunityPageController {

    private static final String WRITE_VIEW = "community/write";

    private final PostService postService;
    private final PostWriteService postWriteService;

    @GetMapping("/community")
    public String listPage(@RequestParam(required = false) PostCategory category,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("posts", postService.getPosts(category, q, page));
        model.addAttribute("category", category);
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("categories", PostCategory.values());
        return "community/list";
    }

    @GetMapping("/community/{postId}")
    public String detailPage(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        Long loginMemberId = loginMember == null ? null : loginMember.getMemberId();
        model.addAttribute("post", postService.getPost(postId, loginMemberId));
        // 댓글 입력: 비회원은 로그인 안내, 미인증 회원은 인증 안내 (U-22 예외, PL-USR-003)
        model.addAttribute("loggedIn", loginMemberId != null);
        model.addAttribute("canComment", loginMemberId != null && postWriteService.canWrite(loginMemberId));
        return "community/detail";
    }

    /* API-CMT-03. 미인증이면 안내 상자를 보여 주고 등록 버튼을 막는다 */
    @GetMapping("/community/write")
    public String writePage(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("postWriteRequest", new PostWriteRequest());
        return writeView(model, loginMember, null);
    }

    /* API-CMT-04 */
    @PostMapping("/community")
    public String write(@Valid @ModelAttribute PostWriteRequest postWriteRequest, BindingResult bindingResult,
                        @AuthenticationPrincipal LoginMember loginMember, Model model) {
        if (bindingResult.hasErrors()) {
            return writeView(model, loginMember, null);
        }
        try {
            Long postId = postWriteService.write(loginMember.getMemberId(), postWriteRequest);
            return "redirect:/community/" + postId;
        } catch (BusinessException e) {
            rejectOnForm(bindingResult, e);
            return writeView(model, loginMember, null);
        }
    }

    /* API-CMT-05 */
    @GetMapping("/community/{postId}/edit")
    public String editPage(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("postWriteRequest", postWriteService.getEditForm(postId, loginMember.getMemberId()));
        return writeView(model, loginMember, postId);
    }

    /* API-CMT-06 */
    @PostMapping("/community/{postId}/edit")
    public String edit(@PathVariable Long postId, @Valid @ModelAttribute PostWriteRequest postWriteRequest,
                       BindingResult bindingResult, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        if (bindingResult.hasErrors()) {
            return writeView(model, loginMember, postId);
        }
        try {
            postWriteService.update(postId, loginMember.getMemberId(), postWriteRequest);
            return "redirect:/community/" + postId;
        } catch (BusinessException e) {
            rejectOnForm(bindingResult, e);
            return writeView(model, loginMember, postId);
        }
    }

    /* API-CMT-07. M-05 에서 확인한 뒤 온다 */
    @PostMapping("/community/{postId}/delete")
    public String delete(@PathVariable Long postId, @AuthenticationPrincipal LoginMember loginMember,
                         RedirectAttributes redirectAttributes) {
        postWriteService.delete(postId, loginMember.getMemberId());
        redirectAttributes.addFlashAttribute("toast", "게시글을 삭제했습니다");
        return "redirect:/community";
    }

    /* postId 가 있으면 수정 화면이다 */
    private String writeView(Model model, LoginMember loginMember, Long postId) {
        model.addAttribute("editing", postId != null);
        model.addAttribute("postId", postId);
        model.addAttribute("canWrite", postWriteService.canWrite(loginMember.getMemberId()));
        model.addAttribute("existingImages", postId == null ? List.of() : postWriteService.getImages(postId, loginMember.getMemberId()));
        return WRITE_VIEW;
    }

    /*
     * 같은 폼에서 고칠 수 있는 오류는 해당 칸(또는 폼 위)의 메시지로 바꾼다.
     * 없는 글(POST_NOT_FOUND)·남의 글(NOT_OWNER)은 폼으로 고칠 수 없으므로 오류 화면으로 보낸다.
     */
    private void rejectOnForm(BindingResult bindingResult, BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code == CommunityErrorCode.POST_NOT_FOUND || code == CommonErrorCode.NOT_OWNER) {
            throw e;
        }
        if (code == CommunityErrorCode.SHOWCASE_LINK_REQUIRED) {
            bindingResult.rejectValue("workUrl", code.toString(), e.getMessage());
        } else if (code == CommonErrorCode.INVALID_FILE) {
            bindingResult.rejectValue("images", code.toString(), e.getMessage());
        } else {
            bindingResult.reject(code.toString(), e.getMessage());
        }
    }
}
