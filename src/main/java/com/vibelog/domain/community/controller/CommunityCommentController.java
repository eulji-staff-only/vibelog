package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dto.CommentWriteRequest;
import com.vibelog.domain.community.model.service.CommentService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * [CommunityCommentController]
 * 역할 : 댓글·답글 쓰기, 댓글 수정·삭제 요청을 받는다 (API-CMT-08~10). 회원만 (SecurityConfig).
 * 책임 : - 끝나면 게시글 상세(U-22)의 그 댓글 위치로 돌아간다 (#comment-{id}). 삭제는 글로 돌아간다.
 *        - 고칠 수 있는 오류(입력·금칙어·미인증·답글 깊이)는 글로 돌아가 그 폼에 메시지와 쓰던 내용을 다시 보여 준다
 *          (방식 ④·③, flash 로 넘긴다). 없는 글·댓글(POST_NOT_FOUND)·남의 댓글(NOT_OWNER)은 오류 화면으로 간다 (방식 ①).
 */
@Controller
@RequiredArgsConstructor
public class CommunityCommentController {

    private static final FormSpot NEW_COMMENT = new FormSpot("new", "comment-form");

    private final CommentService commentService;

    /* API-CMT-08. parentCommentId 가 있으면 답글 */
    @PostMapping("/community/{postId}/comments")
    public String write(@PathVariable Long postId, @Valid @ModelAttribute CommentWriteRequest request, BindingResult bindingResult,
                        @AuthenticationPrincipal LoginMember loginMember, RedirectAttributes redirectAttributes) {
        Long parentId = request.getParentCommentId();
        FormSpot spot = parentId == null ? NEW_COMMENT : new FormSpot("reply-" + parentId, "comment-" + parentId);
        if (bindingResult.hasErrors()) {
            return backToForm(postId, spot, request, firstMessage(bindingResult), redirectAttributes);
        }
        try {
            Long commentId = commentService.write(postId, loginMember.getMemberId(), request.getContent(), parentId);
            // 작성 중 내용 보관 지우기 (#366, detail.html·comment.html data-draft)
            redirectAttributes.addFlashAttribute("draftClear", parentId == null ? "comment." + postId : "reply." + parentId);
            return "redirect:/community/" + postId + "#comment-" + commentId;
        } catch (BusinessException e) {
            // 답글에 답글(REPLY_DEPTH_EXCEEDED)은 화면에 그 입력란이 없으므로 댓글 입력란에 보여 준다 (방식 ③ → 게시글)
            FormSpot at = e.getErrorCode() == CommunityErrorCode.REPLY_DEPTH_EXCEEDED ? NEW_COMMENT : spot;
            return backToForm(postId, at, request, messageOnForm(e), redirectAttributes);
        }
    }

    /* API-CMT-09 */
    @PostMapping("/comments/{commentId}/edit")
    public String edit(@PathVariable Long commentId, @Valid @ModelAttribute CommentWriteRequest request, BindingResult bindingResult,
                       @AuthenticationPrincipal LoginMember loginMember, RedirectAttributes redirectAttributes) {
        FormSpot spot = new FormSpot("edit-" + commentId, "comment-" + commentId);
        if (bindingResult.hasErrors()) {
            return backToForm(commentService.getPostId(commentId), spot, request, firstMessage(bindingResult), redirectAttributes);
        }
        try {
            Long postId = commentService.edit(commentId, loginMember.getMemberId(), request.getContent());
            return "redirect:/community/" + postId + "#comment-" + commentId;
        } catch (BusinessException e) {
            return backToForm(commentService.getPostId(commentId), spot, request, messageOnForm(e), redirectAttributes);
        }
    }

    /* API-CMT-10 */
    @PostMapping("/comments/{commentId}/delete")
    public String delete(@PathVariable Long commentId, @AuthenticationPrincipal LoginMember loginMember) {
        Long postId = commentService.delete(commentId, loginMember.getMemberId());
        return "redirect:/community/" + postId;
    }

    /* 어느 폼에서 왔는지: target 은 화면이 폼을 고르는 이름, anchor 는 돌아갈 위치 */
    private record FormSpot(String target, String anchor) {
    }

    private String backToForm(Long postId, FormSpot spot, CommentWriteRequest request, String message,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("commentTarget", spot.target());
        redirectAttributes.addFlashAttribute("commentError", message);
        redirectAttributes.addFlashAttribute("commentDraft", request.getContent());
        return "redirect:/community/" + postId + "#" + spot.anchor();
    }

    private static String firstMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().get(0).getDefaultMessage();
    }

    /* 폼으로 고칠 수 없는 오류는 그대로 던져 오류 화면으로 보낸다 */
    private static String messageOnForm(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code == CommunityErrorCode.POST_NOT_FOUND || code == CommonErrorCode.NOT_OWNER) {
            throw e;
        }
        return e.getMessage();
    }
}
