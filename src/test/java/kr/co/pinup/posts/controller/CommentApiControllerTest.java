package kr.co.pinup.posts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.controller.CommentApiController;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class CommentApiControllerTest {

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
    public void testCreateComment() throws Exception {
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

        mockMvc.perform(post("/api/comment/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("This is a test comment"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.postId").value(postId));

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
