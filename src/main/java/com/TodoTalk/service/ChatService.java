package com.TodoTalk.service;

import com.TodoTalk.dto.request.CreateChatRequest;
import com.TodoTalk.dto.response.ChatResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.entity.Chat;
import com.TodoTalk.entity.ChatParticipant;
import com.TodoTalk.entity.User;
import com.TodoTalk.repository.ChatRepository;
import com.TodoTalk.repository.ChatParticipantRepository;
import com.TodoTalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(Long userId) {
        List<Chat> chats = chatRepository.findChatsByUserId(userId);
        return chats.stream()
                .map(this::convertToChatResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatResponse createPrivateChat(Long userId1, Long userId2) {
        // Check if private chat already exists
        List<Chat> existingChats = chatRepository.findPrivateChat(userId1, userId2);
        if (!existingChats.isEmpty()) {
            return convertToChatResponse(existingChats.get(0));
        }

        // Get users
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId1));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId2));

        // Create new private chat
        Chat chat = Chat.builder()
                .chatName(null) // Private chats don't have names
                .isGroup(false)
                .build();

        Chat savedChat = chatRepository.save(chat);

        // Add participants
        ChatParticipant participant1 = ChatParticipant.builder()
                .chat(savedChat)
                .user(user1)
                .build();

        ChatParticipant participant2 = ChatParticipant.builder()
                .chat(savedChat)
                .user(user2)
                .build();

        chatParticipantRepository.save(participant1);
        chatParticipantRepository.save(participant2);

        return convertToChatResponse(savedChat);
    }

    @Transactional
    public ChatResponse createGroupChat(String chatName, Long creatorId, List<Long> memberIds) {
        // Get creator
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found: " + creatorId));

        // Create group chat
        Chat chat = Chat.builder()
                .chatName(chatName)
                .isGroup(true)
                .build();

        Chat savedChat = chatRepository.save(chat);

        // Add creator as participant
        ChatParticipant creatorParticipant = ChatParticipant.builder()
                .chat(savedChat)
                .user(creator)
                .build();
        chatParticipantRepository.save(creatorParticipant);

        // Add other members
        for (Long memberId : memberIds) {
            if (!memberId.equals(creatorId)) { // Don't add creator twice
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

                ChatParticipant participant = ChatParticipant.builder()
                        .chat(savedChat)
                        .user(member)
                        .build();
                chatParticipantRepository.save(participant);
            }
        }

        return convertToChatResponse(savedChat);
    }

    @Transactional(readOnly = true)
    public Optional<ChatResponse> getChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .map(this::convertToChatResponse);
    }

    private ChatResponse convertToChatResponse(Chat chat) {
        List<UserResponse> participants = chat.getParticipants().stream()
                .map(cp -> convertToUserResponse(cp.getUser()))
                .collect(Collectors.toList());

        return new ChatResponse(
                chat.getId(),
                chat.getChatName(),
                chat.getIsGroup(),
                chat.getCreatedAt(),
                participants,
                null, // lastMessage - will be implemented later
                0L   // unreadCount - will be implemented later
        );
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
