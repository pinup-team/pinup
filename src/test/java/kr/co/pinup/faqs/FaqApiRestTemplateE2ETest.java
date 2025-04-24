package kr.co.pinup.faqs;

import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpStatus.OK;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FaqApiRestTemplateE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        faqRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @DisplayName("e2e FAQ 전체 데이터를 조회한다.")
    @Test
    void findAll() {
        // Arrange
        Member member = Member.builder()
                .email("test@gmail.com")
                .name("name")
                .nickname("네이버TestMember")
                .providerType(NAVER)
                .providerId("test").role(ROLE_ADMIN)
                .build();
        memberRepository.save(member);

        Faq faq1 = createFaq("question", "answer", USE, member);
        Faq faq2 = createFaq("question", "answer", USE, member);
        faqRepository.saveAll(List.of(faq1, faq2));

        String url = "http://localhost:" + port + "/api/faqs";

        // Act
        ResponseEntity<List<FaqResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FaqResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).hasSize(2);

        List<Faq> faqs = faqRepository.findAllByOrderByCreatedAtDescIdDesc();
        assertThat(faqs).hasSize(2)
                .extracting(Faq::getQuestion, Faq::getAnswer, Faq::getCategory)
                .containsExactly(
                        tuple(faq2.getQuestion(), faq2.getAnswer(), faq2.getCategory()),
                        tuple(faq1.getQuestion(), faq1.getAnswer(), faq1.getCategory()));
    }

    private Faq createFaq(String question, String answer, FaqCategory category, Member member) {
        return Faq.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .member(member)
                .build();
    }
}
