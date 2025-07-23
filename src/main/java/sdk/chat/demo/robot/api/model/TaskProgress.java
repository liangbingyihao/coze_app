package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class TaskProgress {
    @SerializedName("story_name")
    private String storyName;
    private Integer unlocked;
    private Integer total;
    @SerializedName("is_today_unlocked")
    private Boolean isTodayUnLocked;
    private ArrayList<Chapter> chapters;

    private TaskDetail taskDetail;

    public String getStoryName() {
        return storyName;
    }

    public void setStoryName(String storyName) {
        this.storyName = storyName;
    }

    public Integer getUnlocked() {
        return unlocked;
    }

    public void setUnlocked(Integer unlocked) {
        this.unlocked = unlocked;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Boolean getTodayUnLocked() {
        return isTodayUnLocked;
    }

    public void setTodayUnLocked(Boolean todayUnLocked) {
        isTodayUnLocked = todayUnLocked;
    }

    public ArrayList<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(ArrayList<Chapter> chapters) {
        this.chapters = chapters;
    }

    public TaskDetail getTaskDetail() {
        return taskDetail;
    }

    public void setTaskDetail(TaskDetail taskToday) {
        this.taskDetail = taskToday;
    }
}
