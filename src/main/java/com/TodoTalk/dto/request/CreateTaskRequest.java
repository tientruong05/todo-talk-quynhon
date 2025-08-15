package com.TodoTalk.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {
    private Long messageId;
    private Long chatId;
    private String description;
    private LocalDateTime dueDate;
}
