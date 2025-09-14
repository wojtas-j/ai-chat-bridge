package com.wojtasj.aichatbridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a message entity stored in the database for the AI Chat Bridge application.
 * Uses common fields and behavior from {@link BaseMessage}.
 * @since 1.0
 */
@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@SuperBuilder
@Slf4j
public class MessageEntity extends BaseMessage {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    public MessageEntity() {

    }
}
