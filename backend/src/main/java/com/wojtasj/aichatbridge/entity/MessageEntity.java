package com.wojtasj.aichatbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Represents a message entity stored in the database for the AI Chat Bridge application.
 * @since 1.0
 */
@Entity
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Slf4j
public class MessageEntity {
    /**
     * The unique identifier of the message.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The content of the message, must not be blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Content cannot be blank")
    private String content;

    /**
     * The timestamp when the message was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Logs the creation of a new message entity before persisting it to the database.
     * @since 1.0
     */
    @PrePersist
    public void logNewMessage() {
        log.debug("Creating new message entity with content: {}", content);
    }
}
