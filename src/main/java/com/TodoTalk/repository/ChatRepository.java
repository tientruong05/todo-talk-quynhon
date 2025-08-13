package com.TodoTalk.repository;

import com.TodoTalk.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Tìm tất cả chat của một user
    @Query("SELECT DISTINCT c FROM Chat c " +
           "JOIN ChatParticipant cp ON c.id = cp.chat.id " +
           "WHERE cp.user.id = :userId " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    // Tìm chat 1-1 giữa 2 user
    @Query("SELECT c FROM Chat c " +
           "WHERE c.isGroup = false AND " +
           "c.id IN (SELECT cp1.chat.id FROM ChatParticipant cp1 WHERE cp1.user.id = :userId1) AND " +
           "c.id IN (SELECT cp2.chat.id FROM ChatParticipant cp2 WHERE cp2.user.id = :userId2)")
    List<Chat> findPrivateChat(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    List<Chat> findByIsGroupTrue();

    List<Chat> findByIsGroupFalse();
}
