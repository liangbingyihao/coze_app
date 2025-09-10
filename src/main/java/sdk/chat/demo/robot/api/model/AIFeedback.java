package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.demo.robot.handlers.GWThreadHandler;

public class AIFeedback {
    @SerializedName("color_tag")
    private String colorTag;
    private List<List<String>> function;
    private String topic;
    private String tag;
    private String bible;
    private String view;
    private List<String> explore;
    private String prompt;

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public List<List<String>> getFunction() {
        return function;
    }

    public void setFunction(List<List<String>> function) {
        this.function = function;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBible() {
        return bible;
    }

    public void setBible(String bible) {
        this.bible = bible;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<String> getExplore() {
        return explore;
    }

    public void setExplore(List<String> explore) {
        this.explore = explore;
    }
}
