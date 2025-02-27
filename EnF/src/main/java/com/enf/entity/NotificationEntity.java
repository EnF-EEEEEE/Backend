package com.enf.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "notification")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class NotificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationSeq;

  private Long userId;

  private String message;

  private boolean isSent;

  private LocalDateTime createdAt;

}
