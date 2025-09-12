package com.wojtasj.aichatbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a Discord message entity stored in the database for the AI Chat Bridge application.
 * Uses common fields and behavior from {@link BaseMessage}.
 * Includes the Discord user's nickname for tracking.
 * @since 1.0
 */
@Entity
@Table(name = "discord_message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Slf4j
public class DiscordMessageEntity extends BaseMessage{
    /**
     * The Discord user's nickname, must not be blank.
     */
    @Column(name = "discord_nickame", nullable = false)
    @NotBlank(message = "Discord nickame cannot be blank")
    private String discordNick;
}
