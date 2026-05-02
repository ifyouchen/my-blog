package com.myblog.application.assembler;

import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.User;

import java.time.format.DateTimeFormatter;

/**
 * 用户应用组装器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class UserAssembler {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private UserAssembler() {
    }

    /**
     * 将用户领域对象转换为应用 DTO。
     *
     * @param user 用户领域对象
     * @return 用户应用 DTO
     */
    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId().getValue());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail().getValue());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setWebsite(user.getWebsite());
        dto.setGithub(user.getGithub());
        dto.setTwitter(user.getTwitter());
        dto.setLocation(user.getLocation());
        dto.setLastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().format(DATETIME_FORMATTER) : null);
        dto.setLastLoginIp(user.getLastLoginIp());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        return dto;
    }
}
