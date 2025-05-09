package sdk.chat.demo.robot.ui;

import android.content.Context;

import com.stfalcon.chatkit.messages.MessageHolders;

import sdk.chat.demo.pre.R;
import sdk.chat.ui.custom.TextMessageRegistration;

public class GWMessageRegistration extends TextMessageRegistration {
    @Override
    public void onBindMessageHolders(Context context, MessageHolders holders) {
//        holders.setIncomingTextConfig(GW.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
//                .setOutcomingTextConfig(GW.OutcomingMessageViewHolder.class, sdk.chat.ui.R.layout.view_holder_outcoming_text_message);
        holders.setIncomingTextConfig(GW.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
                .setOutcomingTextConfig(GW.OutcomingMessageViewHolder.class, R.layout.item_outcoming_text);
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
