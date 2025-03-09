package com.example.flightapp;

public class TimelineItem {
    private String title;
    private String subTitle;
    private String time;
    private boolean isCompleted;
    // New properties to control line highlighting:
    private boolean topLineHighlighted;
    private boolean bottomLineHighlighted;

    public TimelineItem(String title, String subTitle, String time, boolean isCompleted) {
        this.title = title;
        this.subTitle = subTitle;
        this.time = time;
        this.isCompleted = isCompleted;
        // Default: lines are not highlighted.
        this.topLineHighlighted = false;
        this.bottomLineHighlighted = false;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getTime() {
        return time;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isTopLineHighlighted() {
        return topLineHighlighted;
    }

    public void setTopLineHighlighted(boolean topLineHighlighted) {
        this.topLineHighlighted = topLineHighlighted;
    }

    public boolean isBottomLineHighlighted() {
        return bottomLineHighlighted;
    }

    public void setBottomLineHighlighted(boolean bottomLineHighlighted) {
        this.bottomLineHighlighted = bottomLineHighlighted;
    }
}
