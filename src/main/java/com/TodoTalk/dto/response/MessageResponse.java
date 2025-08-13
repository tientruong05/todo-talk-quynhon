package com.TodoTalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long messageId;
    private Long chatId;
    private UserResponse sender;
    private String content;
    private Boolean isTodoTrigger;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
}
