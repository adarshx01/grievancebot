package com.redressalbot.grievanceredressalbot.repository;

import com.redressalbot.grievanceredressalbot.entity.ChatSession;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);
    List<ChatSession> findByUserAndStatusOrderByUpdatedAtDesc(User user, SessionStatus status);
    Optional<ChatSession> findByUserAndStatus(User user, SessionStatus status);
}