package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;
import com.prolificinteractive.materialcalendarview.CalendarDay;

public class Story {
    private String id;
    private String name;
    @SerializedName("progress_image")
    private String progressImage;

    private TaskProgress taskProcess;

    public Story(String id, String name, String progressImage) {
        this.id = id;
        this.name = name;
        this.progressImage = progressImage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProgressImage() {
        return progressImage;
    }

    public void setProgressImage(String progressImage) {
        this.progressImage = progressImage;
    }

    public TaskProgress getTaskProcess() {
        return taskProcess;
    }

    public void setTaskProcess(TaskProgress taskProcess) {
        this.taskProcess = taskProcess;
    }
}
