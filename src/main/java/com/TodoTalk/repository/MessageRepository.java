package com.TodoTalk.repository;

import com.TodoTalk.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy tin nhắn trong chat với pagination (sử dụng sentAt thay vì createdAt)
    Page<Message> findByChatIdOrderBySentAtDesc(Long chatId, Pageable pageable);

    // Lấy tin nhắn mới nhất của chat
    List<Message> findTop50ByChatIdOrderBySentAtDesc(Long chatId);

    // Tìm tin nhắn có @Todo trigger
    List<Message> findByChatIdAndIsTodoTriggerTrue(Long chatId);

    // Tìm tin nhắn của user trong chat
    List<Message> findByChatIdAndSenderIdOrderBySentAtDesc(Long chatId, Long senderId);

    // Đếm tin nhắn chưa đọc (sử dụng isRead field)
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.chat.id = :chatId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    Long countUnreadMessagesByChatAndUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Lấy tin nhắn chưa đọc
    @Query("SELECT m FROM Message m " +
           "WHERE m.chat.id = :chatId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    List<Message> findUnreadMessagesByChatAndUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Lấy tin nhắn cuối cùng trong chat
    Optional<Message> findFirstByChatIdOrderBySentAtDesc(Long chatId);
}
