package com.myblog.shared.dto;

/**
 * 公开展示用徽章 DTO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class BadgeDisplayDTO {

    private String code;
    private String type;
    private String name;
    private String description;
    private String iconKey;
    private String tone;
    private String rarity;
    private int sortOrder;
    private boolean owned;
    private boolean equipped;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isOwned() { return owned; }
    public void setOwned(boolean owned) { this.owned = owned; }
    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
}
