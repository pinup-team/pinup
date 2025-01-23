package kr.co.pinup.posts.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.controller.CommentApiController;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;



import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;


@ExtendWith(MockitoExtension.class)  // Mockito 확장자를 사용하여 Mockito 테스트를 진행
class CommentApiControllerTest {

    @InjectMocks
    private CommentApiController commentApiController;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();  // ObjectMapper 명시적 초기화
        mockMvc = MockMvcBuilders.standaloneSetup(commentApiController).build();
    }

    @Test
    public void testCreateComment() throws Exception {
        // given
        Long postId = 1L;

        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content("This is a test comment")
                .userId(1L)
                .build();

        CommentResponse commentResponse = CommentResponse.builder()
                .postId(postId)
                .userId(1L)
                .content("This is a test comment")
                .build();

        when(commentService.createComment(eq(postId), any(CreateCommentRequest.class)))
                .thenReturn(commentResponse);

        // when & then
        mockMvc.perform(post("/api/comment/{postId}", postId)  // Correct HTTP method is POST
                        .contentType(MediaType.APPLICATION_JSON)  // Correctly set the content type
                        .content(objectMapper.writeValueAsString(createCommentRequest)))  // Ensure this correctly serializes your object
                .andExpect(status().isCreated())  // Expecting 201 Created
                .andExpect(jsonPath("$.content").value("This is a test comment"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.postId").value(postId));

        // commentService 호출 여부 검증
        verify(commentService).createComment(eq(postId), any(CreateCommentRequest.class));
    }





    @Test
    public void testDeleteComment() throws Exception {

        Long commentId = 1L;

        mockMvc.perform(delete("/api/comment/{commentId}", commentId))
                .andExpect(status().isNoContent());
        verify(commentService, times(1)).deleteComment(commentId);
    }
}
