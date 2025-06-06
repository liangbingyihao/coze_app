package sdk.chat.demo.robot.holder;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;

import com.stfalcon.chatkit.messages.MessageHolders;

import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.chat.ImageMessageOnClickHandler;
import sdk.chat.ui.chat.LocationMessageOnClickHandler;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.custom.ImageMessageRegistration;

public class ImageRegistration extends ImageMessageRegistration {

    @Override
    public void onBindMessageHolders(Context context, MessageHolders holders) {
//        holders.setIncomingTextConfig(GW.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
//                .setOutcomingTextConfig(GW.OutcomingMessageViewHolder.class, sdk.chat.ui.R.layout.view_holder_outcoming_text_message);
//        holders.setIncomingImageHolder(GWView.IncomingMessageViewHolder.class, R.layout.item_incoming_text);
        holders.setOutcomingImageConfig(GWView.OutgoingImageViewHolder.class, R.layout.item_outgoing_image);

    }

    @Override
    public MessageHolder onNewMessageHolder(Message message) {
        if (message.typeIs(MessageType.Image)) {
            return new ImageHolder(message);
        }
        return null;
    }

    @Override
    public boolean onClick(Activity activity, View rootView, Message message) {
        if (!super.onClick(activity, rootView, message)) {
            if (message.typeIs(MessageType.Image)) {
                ImageMessageOnClickHandler.onClick(activity, rootView, message.stringForKey(Keys.ImageUrl));
                return true;
            }
            return false;
        }
        return true;
    }
}
