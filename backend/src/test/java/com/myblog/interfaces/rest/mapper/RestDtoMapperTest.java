package com.myblog.interfaces.rest.mapper;

import com.myblog.application.dto.CommentDTO;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestDtoMapperTest {

    @Test
    void toResponseCopiesCommentStatusAndCanEdit() {
        CommentDTO dto = new CommentDTO();
        dto.setId(1595L);
        dto.setArticleId(100L);
        dto.setUserId(1L);
        dto.setContent("hello");
        dto.setStatus("PUBLISHED");
        dto.setCanEdit(Boolean.TRUE);

        CommentResponse response = new RestDtoMapper().toResponse(dto);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        assertThat(response.getCanEdit()).isTrue();
    }
}
