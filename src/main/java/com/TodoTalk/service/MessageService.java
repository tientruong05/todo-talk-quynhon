package com.TodoTalk.service;

import com.TodoTalk.dto.request.SendMessageRequest;
import com.TodoTalk.dto.response.MessageResponse;
import com.TodoTalk.entity.Chat;
import com.TodoTalk.entity.Message;
import com.TodoTalk.entity.User;
import com.TodoTalk.repository.ChatRepository;
import com.TodoTalk.repository.MessageRepository;
import com.TodoTalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageResponse sendMessage(Long chatId, Long senderId, String content, String messageType) {
        // Get chat and sender
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found: " + senderId));

        // Create message
        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .messageType(messageType)
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);
        return convertToMessageResponse(savedMessage);
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
