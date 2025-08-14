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
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String content;
    private String messageType;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private LocalDateTime readAt;
}
