package com.TodoTalk.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String fullName;
    private String avatarUrl;
}
