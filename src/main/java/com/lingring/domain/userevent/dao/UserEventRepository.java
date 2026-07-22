package com.lingring.domain.userevent.dao;

import com.lingring.domain.userevent.domain.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {
}
