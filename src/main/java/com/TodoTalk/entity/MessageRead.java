package com.TodoTalk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Message_Reads", uniqueConstraints = @UniqueConstraint(name = "idx_message_user", columnNames = {"message_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MessageRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
