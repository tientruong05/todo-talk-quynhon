package com.TodoTalk.controller;

import com.TodoTalk.dto.response.MessageResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.service.MessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<MessageResponse> messages = messageService.getChatMessages(chatId, page, size);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestParam Long chatId,
            @RequestParam String content,
            @RequestParam(defaultValue = "TEXT") String messageType,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            MessageResponse message = messageService.sendMessage(chatId, user.getUserId(), content, messageType);
            
            // Send real-time notification to chat participants
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
            
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/mark-read/{chatId}")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long chatId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        messageService.markMessagesAsRead(chatId, user.getUserId());
        
        // Notify other participants that messages have been read
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read", 
            Map.of("userId", user.getUserId(), "chatId", chatId));
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count/{chatId}")
    public ResponseEntity<Long> getUnreadMessageCount(
            @PathVariable Long chatId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        long count = messageService.getUnreadMessageCount(chatId, user.getUserId());
        return ResponseEntity.ok(count);
    }

    // WebSocket message handling
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> messageData, SimpMessageHeaderAccessor headerAccessor) {
        log.info("=== MESSAGE CONTROLLER: Received WebSocket message ===");
        log.info("Message data: {}", messageData);
        
        try {
            Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
            UserResponse user = null;
            if (sessionAttrs != null) {
                Object u = sessionAttrs.get("user");
                if (u instanceof UserResponse) user = (UserResponse) u;
            }
            if (user == null) {
                log.error("WebSocket sendMessage: user not in session attributes - ignoring message");
                return;
            }
            
            Long chatId = Long.valueOf(messageData.get("chatId").toString());
            String content = messageData.get("content").toString();
            String messageType = messageData.getOrDefault("messageType", "TEXT").toString();
            
            log.info("Processing message - Chat ID: {}, User: {} ({}), Content: '{}', Type: {}", 
                     chatId, user.getUsername(), user.getUserId(), content, messageType);

            MessageResponse message = messageService.sendMessage(chatId, user.getUserId(), content, messageType);
            
            log.info("Message processed, broadcasting to WebSocket topic: /topic/chat/{}", chatId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
        } catch (Exception e) {
            log.error("=== ERROR IN MESSAGE CONTROLLER ===");
            log.error("Error sending message (WebSocket): {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.markRead")
    public void markAsRead(@Payload Map<String, Object> readData) {
        try {
            Long chatId = Long.valueOf(readData.get("chatId").toString());
            Long userId = Long.valueOf(readData.get("userId").toString());

            messageService.markMessagesAsRead(chatId, userId);
            
            // Notify other participants
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read", 
                Map.of("userId", userId, "chatId", chatId));
                
        } catch (Exception e) {
            // Handle error
            System.err.println("Error marking messages as read: " + e.getMessage());
        }
    }
}
