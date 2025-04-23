package kr.co.pinup.faqs;

import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static kr.co.pinup.faqs.model.enums.FaqCategory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FaqTest {

    @DisplayName("FAQ 생성시 작성자가 존재하지 않으면 예외가 발생한다.")
    @Test
    void createFaqWithoutMemberThrowsException() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> Faq.builder()
                .question("question 1")
                .answer("answer 1")
                .category(USE)
                .build())
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("작성자는 필수입니다.");
    }

    @DisplayName("FAQ 수정시 question, answer, category가 변경되어야 한다.")
    @Test
    void changeQuestionAndAnswerAndCategory() {
        // Arrange
        Faq faq = createFaq("question 1", "answer 1");
        FaqUpdateRequest request = createFaqUpdateRequest("update question", "update answer");

        // Act
        faq.update(request);

        // Assert
        assertThat(faq).extracting(Faq::getQuestion, Faq::getAnswer, Faq::getCategory)
                .containsExactly(request.question(), request.answer(), request.category());
    }

    private Faq createFaq(String question, String answer) {
        return Faq.builder()
                .question(question)
                .answer(answer)
                .category(USE)
                .member(mock(Member.class))
                .build();
    }

    private FaqUpdateRequest createFaqUpdateRequest(String question, String answer) {
        return FaqUpdateRequest.builder()
                .question(question)
                .answer(answer)
                .category(USE)
                .build();
    }
}