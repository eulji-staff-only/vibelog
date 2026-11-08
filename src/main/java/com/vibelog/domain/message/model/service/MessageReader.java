package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.dao.NoticeRepository;
import com.vibelog.domain.message.model.dto.NoticeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * [MessageReader]
 * 역할 : 다른 영역이 message 영역 값(최근 공지 등)을 읽어 가는 곳. 읽기 전용이다.
 * 책임 : 다른 영역은 Notice·Repository 를 import 하지 않고 이 클래스만 주입해 쓴다 (08 문서 규칙 1).
 *        메서드는 쓰는 쪽이 필요할 때 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageReader {

    private final NoticeRepository noticeRepository;

    /* 최신순 limit 개 (메인 화면 최근 공지, #203) */
    public List<NoticeSummary> findRecentNotices(int limit) {
        return noticeRepository.findAllByOrderByCreatedAtDescNoticeIdDesc(PageRequest.of(0, limit)).stream()
                .map(NoticeSummary::from)
                .toList();
    }
}
