package com.enf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userSeq;

  @ManyToOne
  @JoinColumn(name = "bird_seq")
  private BirdEntity bird;

  @ManyToOne
  @JoinColumn(name = "role_seq")
  private RoleEntity role;

  @ManyToOne
  @JoinColumn(name = "category_seq")
  private CategoryEntity category;

  private String email;

  private String nickname;

  private int birth;

  private String provider;

  private String providerId;

  private LocalDateTime createAt;

  private LocalDateTime lastLoginAt;

  private String refreshToken;

}