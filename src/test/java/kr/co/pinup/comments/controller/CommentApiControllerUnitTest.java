package kr.co.pinup.comments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentApiControllerUnitTest {

    @InjectMocks
    private CommentApiController commentApiController;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(commentApiController).build();
    }

    @Test
    @DisplayName("댓글 생성 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createComment_whenAuthenticatedUser_givenValidRequest_thenSuccess() throws Exception {
        // Given
        Long postId = 1L;

        Member mockMember = Member.builder()
                .name("행복한 돼지")
                .email("test@example.com")
                .nickname("happyPig")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-id-123")
                .role(MemberRole.ROLE_USER)
                .build();

        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content("This is a test comment")
                .build();

        CommentResponse commentResponse = CommentResponse.builder()
                .postId(postId)
                .member(mockMember)
                .content("This is a test comment")
                .build();

        when(commentService.createComment(any(MemberInfo.class), eq(postId), any(CreateCommentRequest.class)))
                .thenReturn(commentResponse);

        // When
        ResultActions result = mockMvc.perform(post("/api/comment/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCommentRequest)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("This is a test comment"))
                .andExpect(jsonPath("$.member.nickname").value(mockMember.getNickname()))
                .andExpect(jsonPath("$.postId").value(postId));

        verify(commentService).createComment(any(MemberInfo.class), eq(postId), any(CreateCommentRequest.class));
    }

    @Test
    @DisplayName("댓글 삭제 - 인증된 사용자")
    void deleteComment_whenExistingComment_thenNoContent() throws Exception {
        // Given
        Long commentId = 1L;
        doNothing().when(commentService).deleteComment(commentId);

        // When
        ResultActions result = mockMvc.perform(delete("/api/comment/{commentId}", commentId));

        // Then
        result.andExpect(status().isNoContent());
        verify(commentService, times(1)).deleteComment(commentId);
    }

}
