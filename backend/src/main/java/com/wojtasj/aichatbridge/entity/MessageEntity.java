package com.wojtasj.aichatbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
* Entity representing a message in the AI Chat Bridge application.
*/
@Entity
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Slf4j
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Logs the creation of a new message entity.
     */
    @PrePersist
    public void logNewMessage() {
        log.debug("Creating new message entity with content: {}", content);
    }
}
