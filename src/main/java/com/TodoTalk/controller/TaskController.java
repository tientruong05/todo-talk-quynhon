package com.TodoTalk.controller;

import com.TodoTalk.dto.response.TaskResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.enums.TaskStatus;
import com.TodoTalk.service.TaskService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<TaskResponse> createTask(
            @RequestParam Long messageId,
            @RequestParam Long chatId,
            @RequestParam String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            TaskResponse task = taskService.createTask(messageId, user.getUserId(), chatId, description, dueDate);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam TaskStatus status,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            TaskResponse task = taskService.updateTaskStatus(taskId, status);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{taskId}/note")
    public ResponseEntity<TaskResponse> addCompletionNote(
            @PathVariable Long taskId,
            @RequestParam String note,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            TaskResponse task = taskService.addCompletionNote(taskId, note);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<TaskResponse> tasks = taskService.getTasksByUser(user.getUserId());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<TaskResponse>> getChatTasks(
            @PathVariable Long chatId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<TaskResponse> tasks = taskService.getTasksByChat(chatId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my-tasks/chat/{chatId}")
    public ResponseEntity<List<TaskResponse>> getMyTasksInChat(
            @PathVariable Long chatId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<TaskResponse> tasks = taskService.getTasksByUserAndChat(user.getUserId(), chatId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my-tasks/status/{status}")
    public ResponseEntity<List<TaskResponse>> getMyTasksByStatus(
            @PathVariable TaskStatus status,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<TaskResponse> tasks = taskService.getTasksByStatus(user.getUserId(), status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable Long taskId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<TaskResponse> task = taskService.getTaskById(taskId);
        return task.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            HttpSession session) {
        
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
