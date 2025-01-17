package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.service.CommentService;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)  // CommentController만 테스트합니다.
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;  // HTTP 요청을 보내고 결과를 검증하는 MockMvc 객체

    @MockBean
    private CommentService commentService;  // CommentService는 Mock 객체로 주입됩니다.

    private Long postId = 1L; // 테스트용 postId

    // 댓글 생성 테스트
    @Test
    public void testCreateComment() throws Exception {
        // given
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a test comment");
        commentDto.setUserId(1L);

        // when & then
        mockMvc.perform(put("/comment/" + postId)
                        .flashAttr("commentDto", commentDto))  // flash attributes 사용
                .andExpect(status().is3xxRedirection())  // 리다이렉트 상태 확인
                .andExpect(redirectedUrl("/post/detail/" + postId));
    }

    // 댓글 삭제 테스트
    @Test
    public void testDeleteComment() throws Exception {
        // given
        Long commentId = 1L;
        Long postId = 1L;

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.delete("/comment/{commentId}", commentId)  // HTTP DELETE 요청
                        .param("postId", String.valueOf(postId)))  // postId를 요청 파라미터로 넘깁니다.
                .andExpect(status().is3xxRedirection())  // 리디렉션 상태 코드 검증
                .andExpect(redirectedUrl("/post/detail/" + postId));  // 리디렉션 URL 검증

        // commentService.deleteComment 메소드가 1번 호출됐는지 검증
        verify(commentService, times(1)).deleteComment(commentId);
    }
}
