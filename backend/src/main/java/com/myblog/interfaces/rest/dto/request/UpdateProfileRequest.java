package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新个人资料请求.
 * <p>
 * 用户修改个人信息时的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UpdateProfileRequest {

    /** 用户昵称. */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    /** 头像URL地址. */
    @Size(max = 500, message = "头像地址长度不能超过 500")
    private String avatarUrl;

    /** 个人简介. */
    @Size(max = 500, message = "个人简介长度不能超过 500")
    private String bio;

    /** 个人网站地址. */
    @Size(max = 500, message = "个人网站长度不能超过 500")
    private String website;

    /** GitHub 用户名. */
    @Size(max = 200, message = "GitHub 用户名长度不能超过 200")
    private String github;

    /** Twitter 用户名. */
    @Size(max = 200, message = "Twitter 用户名长度不能超过 200")
    private String twitter;

    /** 所在地. */
    @Size(max = 200, message = "所在地长度不能超过 200")
    private String location;

    /**
     * 获取昵称.
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称.
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像URL.
     *
     * @return 头像URL
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像URL.
     *
     * @param avatarUrl 头像URL
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 获取个人简介.
     *
     * @return 个人简介
     */
    public String getBio() {
        return bio;
    }

    /**
     * 设置个人简介.
     *
     * @param bio 个人简介
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * 获取个人网站地址.
     *
     * @return 网站地址
     */
    public String getWebsite() {
        return website;
    }

    /**
     * 设置个人网站地址.
     *
     * @param website 网站地址
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * 获取GitHub用户名.
     *
     * @return GitHub用户名
     */
    public String getGithub() {
        return github;
    }

    /**
     * 设置GitHub用户名.
     *
     * @param github GitHub用户名
     */
    public void setGithub(String github) {
        this.github = github;
    }

    /**
     * 获取Twitter用户名.
     *
     * @return Twitter用户名
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * 设置Twitter用户名.
     *
     * @param twitter Twitter用户名
     */
    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    /**
     * 获取所在地.
     *
     * @return 所在地
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置所在地.
     *
     * @param location 所在地
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
