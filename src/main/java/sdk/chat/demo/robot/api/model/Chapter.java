package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;

public class Chapter {
    @SerializedName("story_name")
    private String storyName;
    @SerializedName("chapter_no")
    private Integer chapterNo;
    private String title;
    @SerializedName("html_content")
    private String content;
    private String date;
    private CalendarDay calendarDay;

    public String getStoryName() {
        return storyName;
    }

    public void setStoryName(String storyName) {
        this.storyName = storyName;
    }

    public Integer getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo) {
        this.chapterNo = chapterNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public CalendarDay getCalendarDay() {
        if (calendarDay == null && !date.isEmpty()) {
            String[] parts = date.split("-");
            calendarDay = CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        return calendarDay;
    }

    public void setCalendarDay(CalendarDay calendarDay) {
        this.calendarDay = calendarDay;
    }
}
