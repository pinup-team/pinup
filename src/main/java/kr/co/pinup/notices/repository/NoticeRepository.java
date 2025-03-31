package kr.co.pinup.notices.repository;

import kr.co.pinup.notices.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByIsDeletedFalseOrderByCreatedAtDescIdDesc();

    Optional<Notice> findByIdAndIsDeletedFalse(Long noticeId);
}
