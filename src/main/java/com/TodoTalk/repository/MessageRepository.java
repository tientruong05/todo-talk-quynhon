package com.TodoTalk.repository;

import com.TodoTalk.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy tin nhắn trong chat với pagination
    Page<Message> findByChatIdOrderByCreatedAtDesc(Long chatId, Pageable pageable);

    // Lấy tin nhắn mới nhất của chat
    List<Message> findTop50ByChatIdOrderByCreatedAtDesc(Long chatId);

    // Tìm tin nhắn có @Todo trigger
    List<Message> findByChatIdAndIsTodoTriggerTrue(Long chatId);

    // Tìm tin nhắn của user trong chat
    List<Message> findByChatIdAndSenderIdOrderByCreatedAtDesc(Long chatId, Long senderId);

    // Đếm tin nhắn chưa đọc
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.chat.id = :chatId " +
           "AND m.id NOT IN " +
           "(SELECT mr.message.id FROM MessageRead mr WHERE mr.user.id = :userId)")
    Long countUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Lấy tin nhắn cuối cùng trong chat
    @Query("SELECT m FROM Message m " +
           "WHERE m.chat.id = :chatId " +
           "ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessage(@Param("chatId") Long chatId);
}
