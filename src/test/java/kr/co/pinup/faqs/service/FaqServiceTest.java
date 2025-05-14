package kr.co.pinup.faqs.service;

import kr.co.pinup.faqs.Faq;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    private static final String FAQ_ERROR_MESSAGE = "FAQ가 존재하지 않습니다.";

    @Mock
    private FaqRepository faqRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FaqService faqService;

    @DisplayName("FAQ 전체를 조회한다.")
    @Test
    void findAll() {
        // Arrange
        final LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        final LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 1, 0, 0);

        Faq faq1 = createFaq("question 1", "answer 1");
        Faq faq2 = createFaq("question 2", "answer 2");
        ReflectionTestUtils.setField(faq1, "createdAt", time1);
        ReflectionTestUtils.setField(faq2, "createdAt", time2);

        List<Faq> faqs = List.of(faq2, faq1);

        given(faqRepository.findAllByOrderByCreatedAtDescIdDesc()).willReturn(faqs);

        // Act
        List<FaqResponse> result = faqService.findAll();

        // Assert
        FaqResponse response = result.get(0);

        assertThat(result).hasSize(2);
        assertThat(response.question()).isEqualTo(faq2.getQuestion());
        assertThat(response.answer()).isEqualTo(faq2.getAnswer());
        assertThat(response.category()).isEqualTo(faq2.getCategory());
        assertThat(response.createdAt()).isEqualTo(faq2.getCreatedAt());

        then(faqRepository).should(times(1))
                .findAllByOrderByCreatedAtDescIdDesc();
    }

    @DisplayName("FAQ ID로 1개의 FAQ를 조회한다.")
    @Test
    void find() {
        // Arrange
        long faqId = 1L;
        Optional<Faq> response = Optional.ofNullable(
                createFaq("question 1", "answer 1"));

        given(faqRepository.findById(faqId)).willReturn(response);

        // Act
        FaqResponse result = faqService.find(faqId);

        // Assert
        Faq faq = response.get();

        assertThat(result).isNotNull()
                .extracting(FaqResponse::question, FaqResponse::answer, FaqResponse::category)
                .containsOnly(faq.getQuestion(), faq.getAnswer(), faq.getCategory());

        then(faqRepository).should(times(1))
                .findById(faqId);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 조회시 예외가 발생한다.")
    @Test
    void findWithNonExistIdThrowException() {
        // Arrange
        long faqId = Long.MAX_VALUE;

        given(faqRepository.findById(faqId)).willThrow(new FaqNotFound());

        // Act & Assert
        assertThatThrownBy(() -> faqService.find(faqId))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(FAQ_ERROR_MESSAGE);

        then(faqRepository).should(times(1))
                .findById(faqId);
    }

    @DisplayName("FAQ를 정상적으로 저장한다.")
    @Test
    void saved() {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        Member member = Member.builder()
                .nickname("nickname")
                .build();
        FaqCreateRequest request = createFaqCreateRequest();

        given(memberRepository.findByNickname(memberInfo.nickname()))
                .willReturn(Optional.ofNullable(member));

        // Act
        faqService.save(memberInfo, request);

        // Assert
        then(faqRepository).should(times(1))
                .save(any(Faq.class));
    }

    @DisplayName("FAQ 저장시에 멤버 정보가 없으면 예외가 발생한다.")
    @Test
    void savingWithoutMemberInfoThrowsException() {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        FaqCreateRequest request = createFaqCreateRequest();

        given(memberRepository.findByNickname(memberInfo.nickname()))
                .willThrow(new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        // Act & Assert
        assertThatThrownBy(() -> faqService.save(memberInfo, request))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage(memberInfo.nickname() + "님을 찾을 수 없습니다.");
    }

    @DisplayName("FAQ를 정상적으로 수정한다.")
    @Test
    void updated() {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest();
        Faq faqMock = mock(Faq.class);

        given(faqRepository.findById(faqId))
                .willReturn(Optional.ofNullable(faqMock));

        // Act
        faqService.update(faqId, request);

        // Assert
        then(faqMock).should(times(1))
                .update(request);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 수정시 예외를 발생한다.")
    @Test
    void updatingWithNonExistIdThrowsException() {
        // Arrange
        long faqId = Long.MAX_VALUE;
        FaqUpdateRequest request = createFaqUpdateRequest();

        given(faqRepository.findById(faqId)).willThrow(new FaqNotFound());

        // Act & Assert
        assertThatThrownBy(() -> faqService.update(faqId, request))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(FAQ_ERROR_MESSAGE);

        then(faqRepository).should(times(1))
                .findById(faqId);
    }

    @DisplayName("FAQ를 정상적으로 삭제한다.")
    @Test
    void deleted() {
        // Arrange
        long faqId = 1L;
        Faq faq = createFaq("question 1", "answer 1");

        given(faqRepository.findById(faqId))
                .willReturn(Optional.ofNullable(faq));

        // Act
        faqService.remove(faqId);

        // Assert
        then(faqRepository).should(times(1))
                .delete(faq);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 삭제시 예외를 발생한다.")
    @Test
    void deletingWithNonExistIdThrowsException() {
        // Arrange
        long faqId = Long.MAX_VALUE;

        given(faqRepository.findById(faqId)).willThrow(new FaqNotFound());

        // Act & Assert
        assertThatThrownBy(() -> faqService.remove(faqId))
                .isInstanceOf(FaqNotFound.class)
                .hasMessage(FAQ_ERROR_MESSAGE);

        then(faqRepository).should(times(1))
                .findById(faqId);
    }

    private Faq createFaq(String question, String answer) {
        return Faq.builder()
                .question(question)
                .answer(answer)
                .category(USE)
                .member(mock(Member.class))
                .build();
    }

    private MemberInfo createMemberInfo() {
        return MemberInfo.builder()
                .nickname("nickname")
                .role(ROLE_ADMIN)
                .build();
    }

    private FaqCreateRequest createFaqCreateRequest() {
        return FaqCreateRequest.builder()
                .question("question 1")
                .answer("answer 1")
                .category(USE)
                .build();
    }

    private FaqUpdateRequest createFaqUpdateRequest() {
        return FaqUpdateRequest.builder()
                .question("update question")
                .answer("update answer")
                .category(USE)
                .build();
    }
}