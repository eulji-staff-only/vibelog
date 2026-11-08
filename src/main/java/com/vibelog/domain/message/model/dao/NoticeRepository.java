package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByCreatedAtDescNoticeIdDesc(Pageable pageable);
}
