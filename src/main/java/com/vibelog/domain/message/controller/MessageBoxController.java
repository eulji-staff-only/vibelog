package com.vibelog.domain.message.controller;

import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dto.MessageContentRequest;
import com.vibelog.domain.message.model.dto.MessageWriteRequest;
import com.vibelog.domain.message.model.service.MessageService;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
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

/*
 * [MessageBoxController]
 * 역할 : 쪽지함(U-27) 화면과 쪽지 보내기·답장, 공개 프로필에서 쪽지 보내기 (API-MSG-01·03·04·05).
 * 책임 : - 왼쪽은 받은·보낸 탭 목록, 오른쪽은 연 쪽지(답장 폼) 또는 새 쪽지 폼이다. messageId 가 없으면 새 쪽지 폼.
 *        - 보내기 검사에 걸리면 같은 폼에 입력값과 메시지를 다시 보여 준다 (방식 ④). 없는 쪽지·남의 쪽지는 오류 화면.
 *        - 공개 프로필(U-29)에서 보내면 프로필로 돌아가고, 실패하면 flash messageContent·messageError 로 M-13 을 다시 연다.
 */
@Controller
@RequiredArgsConstructor
public class MessageBoxController {

    private static final String VIEW = "message/messages";

    private final MessageService messageService;

    @GetMapping("/messages")
    public String box(@RequestParam(defaultValue = MessageService.BOX_RECEIVED) String box,
                      @RequestParam(required = false) Long messageId, @RequestParam(defaultValue = "1") int page,
                      @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("messageWriteRequest", new MessageWriteRequest());
        model.addAttribute("messageContentRequest", new MessageContentRequest());
        return view(loginMember.getMemberId(), box, messageId, page, model);
    }

    @PostMapping("/messages")
    public String send(@Valid @ModelAttribute MessageWriteRequest messageWriteRequest, BindingResult bindingResult,
                       @AuthenticationPrincipal LoginMember loginMember, Model model, RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (!bindingResult.hasErrors()) {
            try {
                Long messageId = messageService.send(memberId, messageWriteRequest.getReceiverMemberId(), messageWriteRequest.getContent());
                redirectAttributes.addFlashAttribute("toast", "쪽지를 보냈습니다");
                redirectAttributes.addFlashAttribute("draftClear", "message");   // 작성 중 내용 보관 지우기 (#366, messages.html data-draft)
                return "redirect:/messages?box=sent&messageId=" + messageId;
            } catch (BusinessException e) {
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        model.addAttribute("messageContentRequest", new MessageContentRequest());
        model.addAttribute("receiverNickname", messageService.getNickname(messageWriteRequest.getReceiverMemberId()));
        return view(memberId, MessageService.BOX_SENT, null, 1, model);
    }

    @PostMapping("/messages/{messageId}/reply")
    public String reply(@PathVariable Long messageId, @Valid @ModelAttribute MessageContentRequest messageContentRequest,
                        BindingResult bindingResult, @AuthenticationPrincipal LoginMember loginMember, Model model,
                        RedirectAttributes redirectAttributes) {
        Long memberId = loginMember.getMemberId();
        if (!bindingResult.hasErrors()) {
            try {
                messageService.reply(memberId, messageId, messageContentRequest.getContent());
                redirectAttributes.addFlashAttribute("toast", "답장을 보냈습니다");
                redirectAttributes.addFlashAttribute("draftClear", "message-reply." + messageId);
                return "redirect:/messages?box=received&messageId=" + messageId;
            } catch (BusinessException e) {
                if (e.getErrorCode() == MessageErrorCode.MESSAGE_NOT_FOUND || e.getErrorCode() == CommonErrorCode.NOT_OWNER) {
                    throw e;
                }
                bindingResult.reject(e.getErrorCode().name(), e.getMessage());
            }
        }
        model.addAttribute("messageWriteRequest", new MessageWriteRequest());
        return view(memberId, MessageService.BOX_RECEIVED, messageId, 1, model);
    }

    /* API-MSG-04 (M-13). 성공·실패 모두 공개 프로필로 돌아간다 */
    @PostMapping("/users/{memberId}/messages")
    public String sendFromProfile(@PathVariable Long memberId, @Valid @ModelAttribute MessageContentRequest messageContentRequest,
                                  BindingResult bindingResult, @AuthenticationPrincipal LoginMember loginMember,
                                  RedirectAttributes redirectAttributes) {
        String error;
        if (bindingResult.hasErrors()) {
            error = MessageContentRequest.CONTENT_RULE;
        } else {
            try {
                messageService.send(loginMember.getMemberId(), memberId, messageContentRequest.getContent());
                redirectAttributes.addFlashAttribute("toast", "쪽지를 보냈습니다");
                redirectAttributes.addFlashAttribute("draftClear", "message." + memberId);   // profile.html M-13 data-draft
                return "redirect:/users/" + memberId;
            } catch (BusinessException e) {
                error = e.getMessage();
            }
        }
        redirectAttributes.addFlashAttribute("messageContent", messageContentRequest.getContent());
        redirectAttributes.addFlashAttribute("messageError", error);
        return "redirect:/users/" + memberId;
    }

    private String view(Long memberId, String box, Long messageId, int page, Model model) {
        String tab = MessageService.BOX_SENT.equals(box) ? MessageService.BOX_SENT : MessageService.BOX_RECEIVED;
        // 먼저 열어 읽음을 남겨야 목록에도 읽은 쪽지로 보인다
        model.addAttribute("opened", messageId == null ? null : messageService.open(memberId, messageId));
        model.addAttribute("box", tab);
        model.addAttribute("messages", messageService.getBox(memberId, tab, page));
        return VIEW;
    }
}
