package com.TodoTalk.service;

import com.TodoTalk.dto.response.MessageResponse;
import com.TodoTalk.dto.response.TaskResponse;
import com.TodoTalk.entity.Chat;
import com.TodoTalk.entity.Message;
import com.TodoTalk.entity.User;
import com.TodoTalk.repository.ChatRepository;
import com.TodoTalk.repository.MessageRepository;
import com.TodoTalk.repository.UserRepository;
import com.TodoTalk.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final TaskService taskService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(Long chatId, Long senderId, String content, String messageType) {
        log.info("=== MESSAGE SERVICE: Sending message ===");
        log.info("Chat ID: {}, Sender ID: {}, Content: '{}', Type: {}", chatId, senderId, content, messageType);
        
        // Get chat and sender
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found: " + senderId));

        log.info("Chat found: {}, Sender found: {}", chat.getId(), sender.getUsername());

        // Create message
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .messageType(messageType)
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved with ID: {}", savedMessage.getId());
        
        // Check if message contains @Todo (case-insensitive) and process it
        if (content != null && content.toLowerCase().contains("@todo")) {
            log.info("=== @TODO DETECTED! Processing todo message (case-insensitive) ===");
            processToDoMessage(savedMessage, chat, sender);
        } else {
            log.info("No @Todo found in message content");
        }
        
        return convertToMessageResponse(savedMessage);
    }
    
    private void processToDoMessage(Message message, Chat chat, User sender) {
        log.info("=== PROCESSING @TODO MESSAGE ===");
        log.info("Message ID: {}, Content: '{}'", message.getId(), message.getContent());
        log.info("Sender: {} (ID: {}), Chat: {} (ID: {})", sender.getUsername(), sender.getId(), 
                 chat.getChatName() != null ? chat.getChatName() : "Private", chat.getId());
        
        try {
            // Analyze message with AI
            log.info("Calling GeminiService.analyzeTaskMessage...");
            GeminiService.TaskAnalysisResult analysis = geminiService.analyzeTaskMessage(message.getContent());
            
            log.info("AI Analysis Result - Description: '{}', DueDate: {}, AI Processed: {}", 
                     analysis.getDescription(), analysis.getDueDate(), analysis.isAiProcessed());
            
            // Create task based on AI analysis
            log.info("Calling TaskService.createTask...");
            TaskResponse newTask = taskService.createTask(
                    message.getId(),
                    sender.getId(),
                    chat.getId(),
                    analysis.getDescription(),
                    analysis.getDueDate()
            );
            
            log.info("Task created successfully with ID: {}", newTask.getTaskId());
            
            // Send real-time notification to chat participants about new task
            String topicPath = "/topic/chat/" + chat.getId() + "/tasks";
            log.info("Sending WebSocket notification to: {}", topicPath);
            messagingTemplate.convertAndSend(topicPath, newTask);
            log.info("WebSocket notification sent successfully");
            
        } catch (Exception e) {
            log.error("=== ERROR PROCESSING @TODO MESSAGE ===");
            log.error("Error details: {}", e.getMessage(), e);
            // Log error but don't fail the message sending
        }
        
        log.info("=== FINISHED PROCESSING @TODO MESSAGE ===");
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatMessages(Long chatId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable);
        
        return messages.getContent().stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(Long chatId, Long userId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByChatAndUser(chatId, userId);
        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
        }
        messageRepository.saveAll(unreadMessages);
    }

    @Transactional(readOnly = true)
    public long getUnreadMessageCount(Long chatId, Long userId) {
        return messageRepository.countUnreadMessagesByChatAndUser(chatId, userId);
    }

    @Transactional(readOnly = true)
    public MessageResponse getLastMessage(Long chatId) {
        return messageRepository.findFirstByChatIdOrderBySentAtDesc(chatId)
                .map(this::convertToMessageResponse)
                .orElse(null);
    }

    private MessageResponse convertToMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSender().getFullName(),
                message.getSender().getAvatarUrl(),
                message.getContent(),
                message.getMessageType(),
                message.getSentAt(),
                message.getIsRead(),
                message.getReadAt()
        );
    }
}
