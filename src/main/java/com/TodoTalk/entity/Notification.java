package com.TodoTalk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.TodoTalk.enums.NotificationType;

@Entity
@Table(name = "Notifications", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private NotificationType type;

    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Column(length = 255)
    private String content;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
