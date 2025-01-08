package kr.co.pinup.users.model;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 추후에 BaseTimeEntity를 상속한 엔티티들을 아래 필드들을 컬럼으로 인식
@EntityListeners(AuditingEntityListener.class)  // Auditing(자동으로 값 매핑) 기능 추가
public abstract class BaseTimeEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
