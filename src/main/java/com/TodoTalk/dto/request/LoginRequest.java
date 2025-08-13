package com.TodoTalk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    private String usernameOrEmail; // Keep field name for compatibility, but now only accepts email
    
    @NotBlank(message = "Password is required")
    private String password;
}
