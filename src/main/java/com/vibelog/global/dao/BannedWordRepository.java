package com.vibelog.global.dao;

import com.vibelog.global.entity.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {
}
