package kr.co.pinup.notices.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NoticeApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class NoticeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeService noticeService;

    @DisplayName("공지사항 전체를 조회한다.")
    @Test
    void findAll() throws Exception {
        // Arrange
        NoticeResponse response1 = createNoticeResponse("title 1", "content 1",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        NoticeResponse response2 = createNoticeResponse("title 2", "content 2",
                LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        List<NoticeResponse> responses = List.of(response2, response1);

        given(noticeService.findAll()).willReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/notices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());

        then(noticeService).should(times(1))
                .findAll();
    }

    @DisplayName("ID로 1개의 공지사항을 조회한다.")
    @Test
    void findById() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeResponse response = createNoticeResponse("title 1", "content 1",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0));

        given(noticeService.find(noticeId)).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 조회시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void findByNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        given(noticeService.find(noticeId)).willThrow(new NoticeNotFound());

        // Act & Assert
        mockMvc.perform(get("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("공지사항을 정상적으로 저장한다.")
    @Test
    void save() throws Exception {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        NoticeCreateRequest request = createRequest("title 1", "content 1");
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(noticeService)
                .save(memberInfo, request);

        // Act & Assert
        mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated());

        then(noticeService).should(times(1))
                .save(any(MemberInfo.class), eq(request));
    }

    @DisplayName("공지사항 저장시 제목은 필수 값이다.")
    @Test
    void invalidTitleToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest(null, "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());
    }

    @DisplayName("공지사항 저장시 제목의 길이는 1~100자 이내이다.")
    @Test
    void invalidTitleLengthToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("A".repeat(101), "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());
    }

    @DisplayName("공지사항 저장시 내용은 필수 값이다.")
    @Test
    void invalidContentToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("title 1", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.content").exists());
    }

    @DisplayName("공지사항을 정상적으로 수정한다.")
    @Test
    void update() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(noticeService)
                .update(noticeId, request);

        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        then(noticeService).should(times(1))
                .update(noticeId, request);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 수정시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void updatingNoticeNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        willThrow(new NoticeNotFound()).given(noticeService)
                .update(noticeId, request);

        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .update(noticeId, request);
    }

    @DisplayName("공지사항 수정시 제목은 필수 값이다.")
    @Test
    void invalidTitleToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest(null, "update content");
        String body = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body)) 
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());
    }

    @DisplayName("공지사항 수정시 제목의 길이는 1~100자 이내이다.")
    @Test
    void invalidTitleLengthToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("A".repeat(101), "update content");
        String body = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body)) 
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());
    }

    @DisplayName("공지사항 수정시 내용은 필수 값이다.")
    @Test
    void invalidContentToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("title update", null);
        String body = objectMapper.writeValueAsString(request);
        
        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body)) 
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.content").exists());
    }

    @DisplayName("공지사항을 정상적으로 삭제한다.")
    @Test
    void remove() throws Exception {
        // Arrange
        long noticeId = 1L;

        willDoNothing().given(noticeService)
                .remove(noticeId);
        
        // Act & Assert
        mockMvc.perform(delete("/api/notices/{noticeId}", noticeId)) 
                .andDo(print())
                .andExpect(status().isNoContent());

        then(noticeService).should(times(1))
                .remove(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 삭제시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void deletingNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        willThrow(new NoticeNotFound()).given(noticeService)
                .remove(noticeId);

        // Act & Assert
        mockMvc.perform(delete("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .remove(noticeId);
    }

    private NoticeResponse createNoticeResponse(String title, String content,
                                                       LocalDateTime dateTime) {
        return NoticeResponse.builder()
                .title(title)
                .content(content)
                .member(mock(MemberResponse.class))
                .createdAt(dateTime).build();
    }

    private MemberInfo createMemberInfo() {
        return MemberInfo.builder()
                .nickname("nickname")
                .provider(NAVER)
                .role(ROLE_ADMIN)
                .build();
    }

    private NoticeCreateRequest createRequest(String title, String content) {
        return NoticeCreateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }

    private NoticeUpdateRequest createUpdateRequest(String title, String content) {
        return NoticeUpdateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }
}
