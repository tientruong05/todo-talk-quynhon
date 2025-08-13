package com.TodoTalk.repository;

import com.TodoTalk.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatId(Long chatId);

    List<ChatParticipant> findByUserId(Long userId);

    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    void deleteByChatIdAndUserId(Long chatId, Long userId);

    // Đếm số thành viên trong chat
    @Query("SELECT COUNT(cp) FROM ChatParticipant cp WHERE cp.chat.id = :chatId")
    Long countParticipantsByChat(@Param("chatId") Long chatId);

    // Lấy thông tin user và thời gian join
    @Query("SELECT cp FROM ChatParticipant cp " +
           "JOIN FETCH cp.user " +
           "WHERE cp.chat.id = :chatId " +
           "ORDER BY cp.joinedAt ASC")
    List<ChatParticipant> findParticipantsWithUserInfo(@Param("chatId") Long chatId);
}
