package com.TodoTalk.controller;

import com.TodoTalk.dto.response.ChatResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<ChatResponse> chats = chatService.getUserChats(user.getUserId());
        return ResponseEntity.ok(chats);
    }

    @PostMapping("/private")
    public ResponseEntity<ChatResponse> createPrivateChat(
            @RequestParam Long otherUserId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ChatResponse chat = chatService.createPrivateChat(user.getUserId(), otherUserId);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/group")
    public ResponseEntity<ChatResponse> createGroupChat(
            @RequestParam String chatName,
            @RequestParam String memberIds,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Parse memberIds string to List<Long>
            List<Long> memberIdList = Arrays.stream(memberIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            ChatResponse chat = chatService.createGroupChat(chatName, user.getUserId(), memberIdList);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(
            @PathVariable Long chatId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<ChatResponse> chat = chatService.getChatById(chatId);
        return chat.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
}
