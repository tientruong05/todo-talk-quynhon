package com.TodoTalk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRequest {

    @NotBlank(message = "Chat name is required for group chats")
    @Size(max = 100, message = "Chat name must not exceed 100 characters")
    private String chatName;

    private Boolean isGroup = false;

    private Long otherUserId; // For 1-1 chat
}
