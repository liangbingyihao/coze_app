package sdk.chat.demo.robot.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import sdk.chat.core.dao.Message;
import sdk.chat.core.handlers.MessageHandler;
import sdk.chat.core.manager.MessagePayload;
import sdk.chat.core.manager.TextMessagePayload;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.model.MessageDetail;

public class GWMsgHandler implements MessageHandler {

    @Override
    public MessagePayload payloadFor(Message message) {
        return new TextMessagePayload(message);
    }

    @Override
    public boolean isFor(MessageType type) {
        return type != null && type.is(MessageType.System);
    }


    public static MessageDetail getAiFeedback(Message message) {
        String aiFeedbackStr = message.stringForKey(GWThreadHandler.KEY_AI_FEEDBACK);
        if (!aiFeedbackStr.isEmpty()) {
            return (new Gson()).fromJson(aiFeedbackStr, MessageDetail.class);
        }
        return null;
    }

}
