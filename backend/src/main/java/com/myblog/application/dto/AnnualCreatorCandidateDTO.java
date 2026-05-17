package com.myblog.application.dto;

/**
 * 年度创作者候选应用数据.
 *
 * @author Codex
 * @since 2026-05-17
 */
public class AnnualCreatorCandidateDTO {

    private Long userId;
    private String username;
    private String nickname;
    private int currentLevel;
    private int currentExp;
    private String grantedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getCurrentExp() { return currentExp; }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }
    public String getGrantedAt() { return grantedAt; }
    public void setGrantedAt(String grantedAt) { this.grantedAt = grantedAt; }
}
