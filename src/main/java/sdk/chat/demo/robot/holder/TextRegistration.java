package sdk.chat.demo.robot.holder;

import android.content.Context;

import com.stfalcon.chatkit.messages.MessageHolders;

import sdk.chat.core.dao.Message;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.custom.TextMessageRegistration;

public class TextRegistration extends TextMessageRegistration {

    @Override
    public void onBindMessageHolders(Context context, MessageHolders holders) {
        holders.setIncomingTextConfig(GWView.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
                .setOutcomingTextConfig(GWView.OutgoingMessageViewHolder.class, R.layout.item_outgoing_text);

    }

    @Override
    public MessageHolder onNewMessageHolder(Message message) {
        if (message.typeIs(MessageType.Text)) {
            return new TextHolder(message);
        }
        return null;
    }
//    public boolean onClick(Activity activity, View rootView, Message message) {
//        if (message.getMessageType().is(MessageType.Text)) {
//            for (MessageMetaValue v : message.getMetaValues()) {
//                if ("action".equals(v.getKey()) && "1".equals(v.getValue())) {
//                    ChatSDK.thread().sendMessageWithText(message.getText(), message.getThread());
//                    break;
//                }
//            }
//            return true;
//        }
//        if (!super.onClick(activity, rootView, message)) {
//            return false;
//        }
//        return true;
//    }
}
