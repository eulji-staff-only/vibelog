package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByCreatedAtDescNoticeIdDesc(Pageable pageable);

    /* 공지 목록 (U-25·A-10) 최신순, 페이지 정보 포함 */
    Page<Notice> findPageByOrderByCreatedAtDescNoticeIdDesc(Pageable pageable);

    /* PL-NTF-003 배너는 동시에 1개: 기간이 겹치는 다른 배너 공지가 있나. 수정할 때는 excludeId(자기 자신)를 뺀다 */
    @Query("select case when count(n) > 0 then true else false end from Notice n"
            + " where n.isBannerShown = true and n.bannerStartsAt < :endsAt and n.bannerEndsAt > :startsAt"
            + " and (:excludeId is null or n.noticeId <> :excludeId)")
    boolean existsBannerOverlap(@Param("startsAt") LocalDateTime startsAt, @Param("endsAt") LocalDateTime endsAt,
                                @Param("excludeId") Long excludeId);

    /* 지금 띄울 배너 (헤더 noticeBanner). 겹침을 막아 보통 1개다 */
    @Query("select n from Notice n where n.isBannerShown = true and n.bannerStartsAt <= :now and n.bannerEndsAt > :now"
            + " order by n.bannerStartsAt desc, n.noticeId desc")
    List<Notice> findShowingBanners(@Param("now") LocalDateTime now, Pageable pageable);
}
