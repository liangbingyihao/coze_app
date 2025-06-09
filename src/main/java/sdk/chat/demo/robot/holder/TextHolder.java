package sdk.chat.demo.robot.holder;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import kotlin.jvm.JvmField;
import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.handlers.GWMsgHandler;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.ui.chat.model.MessageHolder;

public class TextHolder extends MessageHolder implements MessageContentType,AIFeedbackType {
    @JvmField
    private MessageDetail aiFeedback;

//    public void setAiFeedback(String jsonStr) {
//        try {
//            Gson gson = new Gson();
//            aiFeedback = gson.fromJson(jsonStr, MessageDetail.class);
//        } catch (JsonSyntaxException ignored) {
//        }
//    }

    public TextHolder(Message message) {
        super(message);
    }

    public void setAiFeedback(MessageDetail aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public MessageDetail getAiFeedback() {
        if(aiFeedback==null&&!message.stringForKey(GWThreadHandler.KEY_AI_FEEDBACK).isEmpty()){
            aiFeedback = GWMsgHandler.getAiFeedback(message);
        }
        return aiFeedback;
    }

    public void updateNextAndPreviousMessages() {
        this.isLast = false;
//        Message nextMessage = message.getNextMessage();
//        Message previousMessage = message.getPreviousMessage();
//
//        boolean isLast = nextMessage == null;
//        if (isLast != this.isLast) {
//            this.isLast = isLast;
//            isDirty = true;
//        }
//
//        if (!isDirty) {
//            String oldNextMessageId = this.nextMessage != null ? this.nextMessage.getEntityID() : "";
//            String newNextMessageId = nextMessage != null ? nextMessage.getEntityID() : "";
//            isDirty = !oldNextMessageId.equals(newNextMessageId);
//        }
//
//        if (!isDirty) {
//            String oldPreviousMessageId = this.previousMessage != null ? this.previousMessage.getEntityID() : "";
//            String newPreviousMessageId = previousMessage != null ? previousMessage.getEntityID() : "";
//            isDirty = !oldPreviousMessageId.equals(newPreviousMessageId);
//        }
//
//        this.nextMessage = nextMessage;
//        this.previousMessage = previousMessage;
//
//        previousSenderEqualsSender = previousMessage != null && message.getSender().equalsEntity(previousMessage.getSender());
//        nextSenderEqualsSender = nextMessage != null && message.getSender().equalsEntity(nextMessage.getSender());
//
//        DateFormat format = UIModule.shared().getMessageBinder().messageTimeComparisonDateFormat(ChatSDK.ctx());
//        showDate = nextMessage == null || !(format.format(message.getDate()).equals(format.format(nextMessage.getDate())) && nextSenderEqualsSender);
////        isGroup = message.getThread().typeIs(ThreadType.Group);
//
//        Logger.warn("Message: " + message.getText() + ", showDate: " + showDate);
    }

    public void updateReadStatus() {
    }

}
