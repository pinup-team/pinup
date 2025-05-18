package kr.co.pinup.notices;

import io.restassured.RestAssured;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.repository.NoticeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeApiRestAssuredE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        noticeRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @DisplayName("e2e 공지사항 전체 데이터를 조회한다.")
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

        Notice notice1 = createNotice("title 1", "content 1", member);
        Notice notice2 = createNotice("title 2", "content 2", member);
        Notice notice3 = Notice.builder()
                .title("title 3")
                .content("content 3")
                .member(member)
                .isDeleted(true)
                .build();

        noticeRepository.saveAll(List.of(notice1, notice2, notice3));

        // Act & Assert
        RestAssured.given()
                .when()
                .get("/api/notices")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("[0].title", equalTo("title 2"))
                .body("[0].content", equalTo("content 2"));

        List<Notice> notices = noticeRepository.findAllByIsDeletedFalseOrderByCreatedAtDescIdDesc();
        assertThat(notices).hasSize(2)
                .extracting(Notice::getTitle, Notice::getContent)
                .containsExactly(
                        tuple(notice2.getTitle(), notice2.getContent()),
                        tuple(notice1.getTitle(), notice1.getContent()));
    }

    private Notice createNotice(String title, String content, Member member) {
        return Notice.builder()
                .title(title)
                .content(content)
                .member(member)
                .build();
    }
}
