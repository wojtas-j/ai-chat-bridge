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
 * Base class for all message-like entities.
 * Provides common fields and behavior.
 * @since 1.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Slf4j
public abstract class BaseMessage {
    /** Unique identifier for the message. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Message content, cannot be blank. */
    @Column(nullable = false)
    @NotBlank(message = "Content cannot be blank")
    private String content;

    /** Creation timestamp. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Logs creation of a new message. */
    @PrePersist
    public void logNewMessage() {
        log.debug("Creating new message with content: {}", content);
    }
}
