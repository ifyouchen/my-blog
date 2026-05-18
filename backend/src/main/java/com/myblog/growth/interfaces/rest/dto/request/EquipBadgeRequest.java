package com.myblog.growth.interfaces.rest.dto.request;

/**
 * 佩戴徽章请求.
 *
 * @author Codex
 * @since 1.0.0
 */
public class EquipBadgeRequest {

    private String badgeCode;

    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }
}
