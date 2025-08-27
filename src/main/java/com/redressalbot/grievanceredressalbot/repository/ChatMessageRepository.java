package com.redressalbot.grievanceredressalbot.repository;

import com.redressalbot.grievanceredressalbot.entity.ChatMessage;
import com.redressalbot.grievanceredressalbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);
    
    // Add this method for ordered retrieval
    List<ChatMessage> findByChatSessionOrderByMessageOrderAsc(ChatSession chatSession);
}