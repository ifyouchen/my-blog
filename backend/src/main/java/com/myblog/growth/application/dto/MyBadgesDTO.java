package com.myblog.growth.application.dto;

import com.myblog.shared.dto.BadgeDisplayDTO;

import java.util.List;

/**
 * 我的徽章列表 DTO.
 *
 * @author Codex
 * @since 1.0.0
 */
public class MyBadgesDTO {

    private List<BadgeDisplayDTO> badges;
    private BadgeDisplayDTO equippedBadge;

    public List<BadgeDisplayDTO> getBadges() { return badges; }
    public void setBadges(List<BadgeDisplayDTO> badges) { this.badges = badges; }
    public BadgeDisplayDTO getEquippedBadge() { return equippedBadge; }
    public void setEquippedBadge(BadgeDisplayDTO equippedBadge) { this.equippedBadge = equippedBadge; }
}
