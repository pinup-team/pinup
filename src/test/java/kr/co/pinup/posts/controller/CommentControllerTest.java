package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.shaded.com.github.dockerjava.core.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)  // Mockito 확장자를 사용하여 Mockito 테스트를 진행
class CommentControllerTest {

    @InjectMocks
    private CommentController commentController;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();  // ObjectMapper 명시적 초기화
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
    }

    @Test
    public void testCreateComment() throws Exception {
        // given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a test comment");
        commentDto.setUserId(1L);

        CommentDto createdComment = new CommentDto();
        createdComment.setContent("This is a test comment");
        createdComment.setUserId(1L);

        // when
        when(commentService.createComment(any(CommentDto.class))).thenReturn(createdComment);

        // then
        mockMvc.perform(put("/comment/{postId}", postId)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())  // 상태 코드가 201 Created인지 확인
                .andExpect(jsonPath("$.content").value("This is a test comment"))  // 응답 본문에 포함된 내용 확인
                .andExpect(jsonPath("$.userId").value(1L));
        verify(commentService).createComment(any(CommentDto.class));
    }

    @Test
    public void testDeleteComment() throws Exception {

        Long commentId = 1L;

        mockMvc.perform(delete("/comment/{commentId}", commentId))
                .andExpect(status().isOk());
        verify(commentService, times(1)).deleteComment(commentId);
    }
}
