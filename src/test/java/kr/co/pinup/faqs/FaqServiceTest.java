package kr.co.pinup.faqs;

import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.users.Member;
import kr.co.pinup.users.MemberRepository;
import kr.co.pinup.users.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class FaqServiceTest {

    private static final String ERROR_MESSAGE = "FAQ가 존재하지 않습니다.";

    @Autowired
    private FaqService faqService;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        faqRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    static Stream<Arguments> faqProvider() {
        return Stream.of(arguments("use", "이거 어떻게 해요?", "이렇게 하시면 됩니다"));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("faqProvider")
    @DisplayName("FAQ 작성")
    void save(String category, String question, String answer) {
        // given
        Member member = Member.builder()
                .email("user1@gmail.com")
                .name("user1")
                .nickname("user1")
                .role(UserRole.ROLE_USER)
                .build();
        memberRepository.save(member);

        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(category)
                .question(question)
                .answer(answer)
                .build();

        // when
        faqService.save(request);

        // then
        List<FaqResponse> response = faqService.findAll();

        assertThat(faqRepository.count()).isEqualTo(1L);
        assertThat(response.get(0).question()).isEqualTo(request.question());
        assertThat(response.get(0).answer()).isEqualTo(request.answer());
    }

    @Test
    @Transactional
    @DisplayName("FAQ 전체 조회")
    void findAll() {
        // given
        Member member = Member.builder()
                .email("user1@gmail.com")
                .name("user1")
                .nickname("user1")
                .role(UserRole.ROLE_USER)
                .build();
        memberRepository.save(member);

        List<Faq> request = IntStream.range(1, 3)
                .mapToObj(i -> Faq.builder()
                        .category(FaqCategory.USE)
                        .question("이거 어떻게 해야 하나요 " + i + "?")
                        .answer("이렇게 하시면 됩니다. " + i)
                        .member(member)
                        .build())
                .toList();
        faqRepository.saveAll(request);

        // when
        List<FaqResponse> faqs = faqService.findAll();

        // then
        assertThat(faqs.size()).isEqualTo(2);
        assertThat(faqs.get(0).category()).isEqualTo(FaqCategory.USE.getName());
        assertThat(faqs.get(0).question()).isEqualTo("이거 어떻게 해야 하나요 2?");
        assertThat(faqs.get(0).answer()).isEqualTo("이렇게 하시면 됩니다. 2");
    }

    @Test
    @Transactional
    @DisplayName("FAQ 단일 조회")
    void find() {
        // given
        Member member = Member.builder()
                .email("user1@gmail.com")
                .name("user1")
                .nickname("user1")
                .role(UserRole.ROLE_USER)
                .build();
        memberRepository.save(member);

        Faq faq = Faq.builder()
                .category(FaqCategory.USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 하시면 됩니다.")
                .member(member)
                .build();
        faqRepository.save(faq);

        // when
        FaqResponse response = faqService.find(faq.getId());

        // then
        assertThat(response.id()).isEqualTo(faq.getId());
        assertThat(response.category()).isEqualTo(faq.getCategory().getName());
        assertThat(response.question()).isEqualTo(faq.getQuestion());
        assertThat(response.answer()).isEqualTo(faq.getAnswer());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회시 에러")
    void nonExistsIdOfFind() {
        // given
        Long faqId = Long.MAX_VALUE;

        // expected
        assertThatThrownBy(() -> faqService.find(faqId))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(ERROR_MESSAGE);
    }

    @Test
    @Transactional
    @DisplayName("FAQ 수정")
    void update() {
        // given
        Member member = Member.builder()
                .email("user1@gmail.com")
                .name("user1")
                .nickname("user1")
                .role(UserRole.ROLE_USER)
                .build();
        memberRepository.save(member);

        Faq faq = Faq.builder()
                .category(FaqCategory.USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 하시면 됩니다.")
                .member(member)
                .build();
        faqRepository.save(faq);

        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        // when
        faqService.update(faq.getId(), request);

        // then
        FaqResponse response = faqService.find(faq.getId());

        assertThat(response.id()).isEqualTo(faq.getId());
        assertThat(response.category()).isEqualTo(FaqCategory.valueOf(request.category()).getName());
        assertThat(response.question()).isEqualTo(request.question());
        assertThat(response.answer()).isEqualTo(request.answer());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 수정시 에러")
    void updateWithNonExistId() {
        // given
        Long faqId = Long.MAX_VALUE;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        // expected
        assertThatThrownBy(() -> faqService.update(faqId, request))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(ERROR_MESSAGE);
    }

    @Test
    @Transactional
    @DisplayName("FAQ 삭제")
    void remove() {
        // given
        Member member = Member.builder()
                .email("user1@gmail.com")
                .name("user1")
                .nickname("user1")
                .role(UserRole.ROLE_USER)
                .build();
        memberRepository.save(member);

        Faq faq = Faq.builder()
                .category(FaqCategory.USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 하시면 됩니다.")
                .member(member)
                .build();
        faqRepository.save(faq);

        // when
        faqService.remove(faq.getId());

        // then
        assertThat(faqRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 삭제시 에러")
    void deleteWithNonExistId() {
        // given
        Long faqId = Long.MAX_VALUE;

        // expected
        assertThatThrownBy(() -> faqService.remove(faqId))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(ERROR_MESSAGE);
    }
}