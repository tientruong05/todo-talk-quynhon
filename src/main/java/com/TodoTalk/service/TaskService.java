package com.TodoTalk.service;

import com.TodoTalk.dto.response.TaskResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.entity.Chat;
import com.TodoTalk.entity.Message;
import com.TodoTalk.entity.Task;
import com.TodoTalk.entity.User;
import com.TodoTalk.enums.TaskStatus;
import com.TodoTalk.repository.ChatRepository;
import com.TodoTalk.repository.MessageRepository;
import com.TodoTalk.repository.TaskRepository;
import com.TodoTalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public TaskResponse createTask(Long messageId, Long userId, Long chatId, String description, LocalDateTime dueDate) {
        log.info("=== TASK SERVICE: Creating task ===");
        log.info("Message ID: {}, User ID: {}, Chat ID: {}", messageId, userId, chatId);
        log.info("Task Description: '{}', Due Date: {}", description, dueDate);
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
        log.info("Message found: ID {}, Content: '{}'", message.getId(), message.getContent());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        log.info("User found: {} ({})", user.getUsername(), user.getId());
        
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));
        log.info("Chat found: ID {}, Name: {}", chat.getId(), 
                 chat.getChatName() != null ? chat.getChatName() : "Private");

        log.info("Building Task entity...");
        Task task = Task.builder()
                .message(message)
                .user(user)
                .chat(chat)
                .description(description)
                .status(TaskStatus.pending)
                .dueDate(dueDate)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Saving task to database...");
        Task savedTask = taskRepository.save(task);
        log.info("Task saved successfully with ID: {}", savedTask.getId());
        
        TaskResponse response = convertToTaskResponse(savedTask);
        log.info("Task converted to response: {}", response.getTaskId());
        log.info("=== TASK SERVICE: Task creation completed ===");
        
        return response;
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        
        if (status == TaskStatus.completed) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToTaskResponse(updatedTask);
    }

    @Transactional
    public TaskResponse addCompletionNote(Long taskId, String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setCompletionNote(note);
        task.setNoteAddedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        Task updatedTask = taskRepository.save(task);
        return convertToTaskResponse(updatedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUser(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByChat(Long chatId) {
        List<Task> tasks = taskRepository.findByChatIdOrderByCreatedAtDesc(chatId);
        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUserAndChat(Long userId, Long chatId) {
        List<Task> tasks = taskRepository.findByUserIdAndChatIdOrderByCreatedAtDesc(userId, chatId);
        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(Long userId, TaskStatus status) {
        List<Task> tasks = taskRepository.findByUserIdAndStatus(userId, status);
        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponse> getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .map(this::convertToTaskResponse);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    private TaskResponse convertToTaskResponse(Task task) {
        UserResponse userResponse = new UserResponse(
                task.getUser().getId(),
                task.getUser().getUsername(),
                task.getUser().getEmail(),
                task.getUser().getFullName(),
                task.getUser().getAvatarUrl(),
                task.getUser().getCreatedAt(),
                task.getUser().getUpdatedAt()
        );

        return new TaskResponse(
                task.getId(),
                task.getMessage().getId(),
                userResponse,
                task.getChat().getId(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getCompletionNote(),
                task.getNoteAddedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}
