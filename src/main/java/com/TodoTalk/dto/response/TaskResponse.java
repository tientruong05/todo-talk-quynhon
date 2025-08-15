package com.TodoTalk.dto.response;

import com.TodoTalk.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long taskId;
    private Long messageId;
    private UserResponse user;
    private Long chatId;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
    private String completionNote;
    private LocalDateTime noteAddedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
