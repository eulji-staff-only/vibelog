package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
