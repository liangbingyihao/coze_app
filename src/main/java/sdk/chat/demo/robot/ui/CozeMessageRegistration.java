package sdk.chat.demo.robot.ui;

import android.app.Activity;
import android.view.View;

import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.MessageMetaValue;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageType;
import sdk.chat.ui.custom.TextMessageRegistration;

public class CozeMessageRegistration extends TextMessageRegistration {
    public boolean onClick(Activity activity, View rootView, Message message) {
        if (message.getMessageType().is(MessageType.Text)) {
            for (MessageMetaValue v : message.getMetaValues()) {
                if ("action".equals(v.getKey()) && "1".equals(v.getValue())) {
                    ChatSDK.thread().sendMessageWithText(message.getText(), message.getThread());
                    break;
                }
            }
            return true;
        }
        if (!super.onClick(activity, rootView, message)) {
            return false;
        }
        return true;
    }
}
