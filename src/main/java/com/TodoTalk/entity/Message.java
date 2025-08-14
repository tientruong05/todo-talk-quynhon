package com.TodoTalk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Messages", indexes = {
        @Index(name = "idx_chat_id", columnList = "chat_id"),
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "message_type", length = 20)
    private String messageType = "TEXT"; // TEXT, IMAGE, FILE, etc.

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder.Default
    @Column(name = "is_todo_trigger")
    private Boolean isTodoTrigger = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageRead> reads = new ArrayList<>();

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY)
    private Task task; // optional

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
