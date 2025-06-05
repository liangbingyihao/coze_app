package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

public class MessageDetail {
    private int status;
    private String summary;
    @SerializedName("color_tag")
    private Long SessionId;

    @SerializedName("feedback_text")
    private String feedbackText;

    private AIFeedback feedback;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getSessionId() {
        return SessionId;
    }

    public void setSessionId(Long sessionId) {
        SessionId = sessionId;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public AIFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(AIFeedback feedback) {
        this.feedback = feedback;
    }
}
