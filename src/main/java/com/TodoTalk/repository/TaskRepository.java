package com.TodoTalk.repository;

import com.TodoTalk.entity.Task;
import com.TodoTalk.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Task> findByChatIdOrderByCreatedAtDesc(Long chatId);

    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);

    List<Task> findByChatIdAndStatus(Long chatId, TaskStatus status);

    // Task quá hạn
    @Query("SELECT t FROM Task t WHERE t.status = :status " +
           "AND t.dueDate < :currentTime")
    List<Task> findOverdueTasks(@Param("status") TaskStatus status, @Param("currentTime") LocalDateTime currentTime);

    // Task sắp hết hạn (trong 24h)
    @Query("SELECT t FROM Task t WHERE t.status = :status " +
           "AND t.dueDate BETWEEN :currentTime AND :endTime")
    List<Task> findTasksDueSoon(@Param("status") TaskStatus status,
                               @Param("currentTime") LocalDateTime currentTime,
                               @Param("endTime") LocalDateTime endTime);

    // Task của user trong chat
    List<Task> findByUserIdAndChatIdOrderByCreatedAtDesc(Long userId, Long chatId);

    // Thống kê task theo trạng thái
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.status = :status")
    Long countTasksByUserAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.chat.id = :chatId AND t.status = :status")
    Long countTasksByChatAndStatus(@Param("chatId") Long chatId, @Param("status") TaskStatus status);
}
