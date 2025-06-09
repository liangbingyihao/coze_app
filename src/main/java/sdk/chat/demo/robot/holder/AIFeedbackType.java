package sdk.chat.demo.robot.holder;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import sdk.chat.demo.robot.api.model.MessageDetail;

public interface AIFeedbackType {
    public void setAiFeedback(MessageDetail aiFeedback);
}
