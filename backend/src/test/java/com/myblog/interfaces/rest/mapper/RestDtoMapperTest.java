package com.myblog.interfaces.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.dto.ArticleSummaryDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CommentDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.interfaces.rest.dto.response.AuthorRankingResponse;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
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

    @Test
    void toPublicResponseHidesSensitiveUserFields() throws JsonProcessingException {
        UserDTO dto = createUserDTO();

        UserResponse response = new RestDtoMapper().toPublicResponse(dto);
        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(response.getEmail()).isNull();
        assertThat(response.getLastLoginAt()).isNull();
        assertThat(response.getLastLoginIp()).isNull();
        assertThat(response.getUsername()).isEqualTo("demo");
        assertThat(response.getNickname()).isEqualTo("Demo User");
        assertThat(response.getFollowed()).isFalse();
        assertThat(json).doesNotContain("email");
        assertThat(json).doesNotContain("lastLoginAt");
        assertThat(json).doesNotContain("lastLoginIp");
    }

    @Test
    void toResponseKeepsPrivateUserFields() {
        UserDTO dto = createUserDTO();

        UserResponse response = new RestDtoMapper().toResponse(dto);

        assertThat(response.getEmail()).isEqualTo("demo@example.com");
        assertThat(response.getLastLoginAt()).isEqualTo("2026-05-05 10:20:30");
        assertThat(response.getLastLoginIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void toAuthorRankingResponseCopiesTopArticle() {
        AuthorRankingDTO dto = new AuthorRankingDTO();
        dto.setRank(1);
        dto.setUser(createUserDTO());
        ArticleSummaryDTO topArticle = new ArticleSummaryDTO();
        topArticle.setId(206L);
        topArticle.setTitle("Go 并发标准库实战");
        topArticle.setSlug("go-206");
        dto.setTopArticle(topArticle);

        AuthorRankingResponse response = new RestDtoMapper().toPublicResponse(dto);

        assertThat(response.getTopArticle()).isNotNull();
        assertThat(response.getTopArticle().getId()).isEqualTo(206L);
        assertThat(response.getTopArticle().getTitle()).isEqualTo("Go 并发标准库实战");
        assertThat(response.getTopArticle().getSlug()).isEqualTo("go-206");
    }

    private UserDTO createUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setUsername("demo");
        dto.setEmail("demo@example.com");
        dto.setNickname("Demo User");
        dto.setAvatarUrl("https://example.com/avatar.png");
        dto.setBio("hello");
        dto.setLastLoginAt("2026-05-05 10:20:30");
        dto.setLastLoginIp("127.0.0.1");
        return dto;
    }
}
