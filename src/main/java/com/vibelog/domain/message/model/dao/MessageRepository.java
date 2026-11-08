package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
