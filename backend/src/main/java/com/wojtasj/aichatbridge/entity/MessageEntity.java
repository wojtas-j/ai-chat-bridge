package com.wojtasj.aichatbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a message entity stored in the database for the AI Chat Bridge application.
 * Uses common fields and behavior from {@link BaseMessage}.
 * @since 1.0
 */
@Entity
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Slf4j
public class MessageEntity extends BaseMessage{
}
