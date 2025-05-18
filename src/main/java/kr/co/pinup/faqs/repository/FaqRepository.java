package kr.co.pinup.faqs.repository;

import kr.co.pinup.faqs.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByCreatedAtDescIdDesc();
}
