package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

public class MessageDetail {
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_CANCEL = 6;
    private String id;
    private int status;
    private String summary;
    @SerializedName("session_id")
    private Long sessionId;

    private String content;

    @SerializedName("feedback_text")
    private String feedbackText;

    private AIFeedback feedback;
    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
//    status_init = 0
//    status_pending = 1
//    status_success = 2
//    status_del = 3
//    status_err = 4
//    status_timeout = 5
//    status_cancel = 6
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
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
