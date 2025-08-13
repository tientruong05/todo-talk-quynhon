package com.TodoTalk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private UserResponse user;
    
    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }
}
