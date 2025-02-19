package com.enf.model.dto.request.auth;

import com.enf.Entity.UserEntity;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
public class KakaoUserDetailsDTO {

  private Map<String, Object> attributes;

  public String getProviderId() {
    return attributes.get("id").toString();
  }

  public String getProvider() {
    return "kakao";
  }

  public String getEmail() {
    return (String) ((Map<?, ?>) attributes.get("kakao_account")).get("email");
  }
  public String getBirthyear() {
    return (String) ((Map<?, ?>) attributes.get("kakao_account")).get("birthyear");
  }

  public String getNickname() {
    return (String) ((Map<?, ?>) attributes.get("properties")).get("nickname");
  }

  public static UserEntity of(KakaoUserDetailsDTO userInfo) {
    LocalDateTime now = LocalDateTime.now();
    return UserEntity.builder()
            .email(userInfo.getEmail())
            .nickname(userInfo.getNickname())
            .providerId(userInfo.getProviderId())
            .provider(userInfo.getProvider())
            .createAt(now)
            .lastLoginAt(now)
            .build();
  }
}
