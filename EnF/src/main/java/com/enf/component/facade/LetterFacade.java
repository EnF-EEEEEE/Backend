package com.enf.component.facade;

import com.enf.entity.*;
import com.enf.exception.GlobalException;
import com.enf.model.dto.request.letter.ReplyLetterDTO;
import com.enf.model.dto.response.PageResponse;
import com.enf.model.dto.response.letter.LetterDetailsDTO;
import com.enf.model.dto.response.letter.ReceiveLetterDTO;
import com.enf.model.dto.response.letter.ThrowLetterCategoryDTO;
import com.enf.model.type.FailedResultType;
import com.enf.model.type.LetterListType;
import com.enf.repository.*;
import com.enf.repository.querydsl.LetterQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LetterFacade {

  private final LetterRepository letterRepository;
  private final NotificationRepository notificationRepository;
  private final LetterStatusRepository letterStatusRepository;
  private final LetterQueryRepository letterQueryRepository;
  private final ThrowLetterRepository throwLetterRepository;
  private final ThrowLetterCategoryRepository throwLetterCategoryRepository;
  private final UserFacade userFacade;

  /**
   * 특정 사용자의 모든 알림 조회
   *
   * @param userSeq 사용자 ID
   * @return 사용자의 모든 알림 리스트
   */
  public List<NotificationEntity> findAllByUserSeq(Long userSeq) {
    return notificationRepository.findAllByUserSeq(userSeq);
  }

  /**
   * 특정 사용자의 모든 알림 삭제
   *
   * @param userSeq 사용자 ID
   */
  public void deleteAllByUserSeq(Long userSeq) {
    notificationRepository.deleteAllByUserSeq(userSeq);
  }

  /**
   * 알림 저장
   *
   * @param notification 저장할 알림 엔티티
   */
  public void saveNotification(NotificationEntity notification) {
    notificationRepository.save(notification);
  }

  /**
   * 멘티가 보낸 편지를 저장하고 편지 상태(LetterStatusEntity)도 함께 저장
   *
   * @param letter 저장할 편지 엔티티
   * @param mentee 편지를 보낸 멘티
   * @param mentor 편지를 받는 멘토
   * @return 저장된 LetterStatusEntity 객체
   */
  public LetterStatusEntity saveMenteeLetter(LetterEntity letter, UserEntity mentee, UserEntity mentor) {
    userFacade.reduceQuota(mentee);
    return letterStatusRepository.save(LetterStatusEntity.of(letterRepository.save(letter), mentee, mentor));
  }

  /**
   * 멘토가 답장을 보낼 때 편지 저장 및 상태 업데이트
   *
   * @param letterStatus 저장할 멘토의 답장 편지 엔티티
   * @param replyLetter 멘티가 보낸 원본 편지 엔티티
   */
  public void saveMentorLetter(LetterStatusEntity letterStatus, ReplyLetterDTO replyLetter) {
    userFacade.reduceQuota(letterStatus.getMentor());
    LetterEntity mentorLetter = letterRepository.save(ReplyLetterDTO.of(replyLetter, letterStatus.getMenteeLetter().getCategoryName()));
    letterStatusRepository.updateLetterStatus(letterStatus.getLetterStatusSeq(), mentorLetter);
  }

  /**
   * 특정 사용자의 모든 편지를 조회하는 기능 (멘티와 멘토 구분하여 처리)
   *
   * @param user 현재 로그인한 사용자 (멘티 또는 멘토)
   * @param pageNumber 요청한 페이지 번호
   * @param letterListType 조회할 편지 유형 (ALL, PENDING, SAVE)
   * @return 페이지네이션이 적용된 편지 목록
   */
  public PageResponse<ReceiveLetterDTO> getLetterList(UserEntity user, int pageNumber, LetterListType letterListType) {
    return letterQueryRepository.getLetterList(user, pageNumber, letterListType.getValue());
  }

  /**
   * 특정 사용자가 편지를 저장하는 기능
   *
   * @param user 현재 로그인한 사용자 (멘티 또는 멘토)
   * @param letterStatusSeq 저장할 편지의 고유 식별자 (ID)
   */
  public void saveLetter(UserEntity user, Long letterStatusSeq) {
    if (user.getRole().getRoleName().equals("MENTEE")) {
      letterStatusRepository.saveLetterForMentee(letterStatusSeq);
    } else {
      letterStatusRepository.saveLetterForMentor(letterStatusSeq);
    }
  }

  /**
   * 특정 편지 상태 정보를 조회
   *
   * @param letterStatusSeq 편지 상태 ID
   * @return 조회된 LetterStatusEntity 객체
   */
  public LetterStatusEntity getLetterStatus(Long letterStatusSeq) {
    return letterStatusRepository.findLetterStatusByLetterStatusSeq(letterStatusSeq)
        .orElseThrow(() -> new GlobalException(FailedResultType.LETTER_NOT_FOUND));
  }

  /**
   * 편지 상세 정보 조회 메서드
   *
   * @param user 요청한 사용자 (멘티 또는 멘토)
   * @param letterStatus 조회할 편지
   * @return 편지 상세 정보를 포함하는 LetterDetailsDTO
   */
  public LetterDetailsDTO getLetterDetails(UserEntity user, LetterStatusEntity letterStatus) {
    if (user.getRole().getRoleName().equals("MENTEE")) {
      if (!letterStatus.isMenteeRead()) {
        letterStatusRepository.updateIsMenteeRead(letterStatus.getLetterStatusSeq());
      }
      return LetterDetailsDTO.ofMentee(letterStatus);
    } else {
      if (!letterStatus.isMentorRead()) {
        letterStatusRepository.updateIsMentorRead(letterStatus.getLetterStatusSeq());
      }
      return LetterDetailsDTO.ofMentor(letterStatus);
    }
  }

  /**
   * 편지를 새로운 멘토에게 전달하는 메서드
   *
   * @param letterStatus 현재 편지의 상태 정보
   */
  public void throwLetter(LetterStatusEntity letterStatus) {
    throwLetterRepository.save(
        ThrowLetterEntity.builder()
            .letterStatus(letterStatus)
            .throwUser(letterStatus.getMentor())
            .build());

    letterQueryRepository.incrementCategory(
        getThrowLetterCategory().getThrowLetterCategorySeq(),
        letterStatus.getMenteeLetter().getCategoryName());
  }

  /**
   * 편지의 멘토를 새로운 멘토로 변경
   *
   * @param letterStatus 현재 편지의 상태 정보
   * @param newMentor 새로운 멘토 정보
   */
  public void updateMentor(LetterStatusEntity letterStatus, UserEntity newMentor) {
    letterStatusRepository.updateMentor(letterStatus.getLetterStatusSeq(), newMentor);
  }

  /**
   * 고마움 전달을 위한 메서드
   *
   * @param letterSeq 고마움을 전달할 멘토 편지의 고유 식별자
   */
  public LetterStatusEntity thanksToMentor(Long letterSeq) {
    LetterStatusEntity letterStatus = letterStatusRepository.getLetterStatusByMentorLetterLetterSeq(letterSeq);
    letterStatusRepository.updateIsThankToMentor(letterStatus.getLetterStatusSeq());
    return letterStatus;
  }

  /**
   * 편지 카테고리 정보를 조회하는 메서드
   *
   * @return ThrowLetterCategoryEntity 객체
   */
  public ThrowLetterCategoryEntity getThrowLetterCategory() {
    return throwLetterCategoryRepository.findByThrowLetterCategorySeq(1L)
        .orElseGet(() -> throwLetterCategoryRepository.save(ThrowLetterCategoryDTO.create()));
  }
}
