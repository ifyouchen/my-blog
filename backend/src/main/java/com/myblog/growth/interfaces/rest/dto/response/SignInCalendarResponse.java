package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到日历响应 DTO.
 * <p>对应 GET /api/points/sign-in/calendar 接口的响应体。</p>
 */
public class SignInCalendarResponse {

    /** 查询月份（格式 yyyy-MM）. */
    private String month;

    /** 本月已签到日期列表. */
    private List<LocalDate> signedDates;

    /** 当前连续签到天数. */
    private int currentConsecutiveDays;

    /** 本月总签到天数. */
    private int totalSignDaysThisMonth;

    /** 今日是否已签到. */
    private boolean todaySigned;

    /** 默认构造. */
    public SignInCalendarResponse() {
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public List<LocalDate> getSignedDates() { return signedDates; }
    public void setSignedDates(List<LocalDate> signedDates) { this.signedDates = signedDates; }

    public int getCurrentConsecutiveDays() { return currentConsecutiveDays; }
    public void setCurrentConsecutiveDays(int currentConsecutiveDays) {
        this.currentConsecutiveDays = currentConsecutiveDays;
    }

    public int getTotalSignDaysThisMonth() { return totalSignDaysThisMonth; }
    public void setTotalSignDaysThisMonth(int totalSignDaysThisMonth) {
        this.totalSignDaysThisMonth = totalSignDaysThisMonth;
    }

    public boolean isTodaySigned() { return todaySigned; }
    public void setTodaySigned(boolean todaySigned) { this.todaySigned = todaySigned; }
}

