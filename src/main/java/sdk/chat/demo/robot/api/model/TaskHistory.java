package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class TaskHistory {
    private ArrayList<Chapter> history;

    public ArrayList<Chapter> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<Chapter> history) {
        this.history = history;
    }
}
