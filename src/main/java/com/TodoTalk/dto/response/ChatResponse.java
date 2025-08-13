package com.TodoTalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long chatId;
    private String chatName;
    private Boolean isGroup;
    private LocalDateTime createdAt;
    private List<UserResponse> participants;
    private MessageResponse lastMessage;
    private Long unreadCount;
}
