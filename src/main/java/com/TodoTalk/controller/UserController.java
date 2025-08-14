package com.TodoTalk.controller;

import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String searchTerm) {
        System.out.println("Search request received for: " + searchTerm);
        List<UserResponse> users = userService.searchUsers(searchTerm);
        System.out.println("Found " + users.size() + " users");
        return ResponseEntity.ok(users);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/not-in-chat/{chatId}")
    public ResponseEntity<List<UserResponse>> getUsersNotInChat(@PathVariable Long chatId) {
        List<UserResponse> users = userService.getUsersNotInChat(chatId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        Optional<UserResponse> user = userService.getUserById(userId);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        Optional<UserResponse> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {
        UserResponse u = (UserResponse) session.getAttribute("user");
        if (u == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(u);
    }
}
