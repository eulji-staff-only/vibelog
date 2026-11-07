package com.vibelog.global.policy;

import com.vibelog.global.dao.BannedWordRepository;
import com.vibelog.global.entity.BannedWord;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * [BannedWordPolicy]
 * 역할 : 사용자가 쓴 글에 금칙어가 들어 있으면 저장을 막는다. (PL-SEC-003, NFR-SEC-009)
 * 책임 : - 닉네임·게시글·댓글·포트폴리오 등 모든 영역이 이 클래스 하나로 검사한다. 그래야 목록과 판단 방식이 같다.
 *        - 여러 칸을 한 번에 받는다. 비어 있는(null) 칸은 건너뛴다.
 *        - 금칙어가 글 안 어디에든 들어 있으면 걸린다. 대소문자는 구분하지 않는다 (banned_word 의 UNIQUE 도 대소문자를 구분하지 않는다).
 *        - 걸리면 BusinessException(BANNED_WORD) 을 던진다.
 */
@Component
@RequiredArgsConstructor
public class BannedWordPolicy {

    private final BannedWordRepository bannedWordRepository;

    public void check(String... texts) {
        // ponytail: 검사할 때마다 금칙어 전체를 읽는다. 목록이 수천 개로 커지면 캐시로 바꾼다
        List<String> words = bannedWordRepository.findAll().stream()
                .map(BannedWord::getWord)
                .filter(word -> !word.isBlank())   // 빈 금칙어는 모든 글에 들어 있다고 판단되므로 뺀다
                .map(word -> word.toLowerCase(Locale.ROOT))
                .toList();
        boolean found = Arrays.stream(texts)
                .filter(Objects::nonNull)
                .map(text -> text.toLowerCase(Locale.ROOT))
                .anyMatch(text -> words.stream().anyMatch(text::contains));
        if (found) {
            throw new BusinessException(CommonErrorCode.BANNED_WORD);
        }
    }
}
