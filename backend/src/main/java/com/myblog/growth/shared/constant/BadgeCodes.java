package com.myblog.growth.shared.constant;

/**
 * 徽章编码常量.
 *
 * @author Codex
 * @since 1.0.0
 */
public final class BadgeCodes {

    public static final String ANNUAL_CREATOR_CANDIDATE = "ANNUAL_CREATOR_CANDIDATE";

    private BadgeCodes() {
    }

    /**
     * 等级徽章编码.
     *
     * @param level 等级
     * @return 徽章编码
     */
    public static String level(int level) {
        return "LEVEL_" + level;
    }

    /**
     * 累计签到徽章编码.
     *
     * @param days 累计签到天数
     * @return 徽章编码
     */
    public static String sign(int days) {
        return "SIGN_" + days;
    }
}
