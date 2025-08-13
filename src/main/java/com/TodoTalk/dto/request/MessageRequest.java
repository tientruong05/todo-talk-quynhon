package com.TodoTalk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    @NotNull(message = "Chat ID is required")
    private Long chatId;

    @NotBlank(message = "Message content is required")
    private String content;
}
