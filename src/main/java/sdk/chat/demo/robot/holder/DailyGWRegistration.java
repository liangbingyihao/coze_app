package sdk.chat.demo.robot.holder;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.stfalcon.chatkit.messages.MessageHolders;

import java.util.List;

import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.ImageViewerActivity;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.ui.listener.ImageMessageOnClickHandler;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.custom.ImageMessageRegistration;

public class DailyGWRegistration extends ImageMessageRegistration {
    public static int GWMessageType = 777;

    @Override
    public List<Byte> getTypes() {
        return types(GWMessageType);
    }

    @Override
    public void onBindMessageHolders(Context context, MessageHolders holders) {
//        holders.setIncomingTextConfig(GW.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
//                .setOutcomingTextConfig(GW.OutcomingMessageViewHolder.class, sdk.chat.ui.R.layout.view_holder_outcoming_text_message);
//        holders.setIncomingImageHolder(GWView.IncomingMessageViewHolder.class, R.layout.item_incoming_text);
//        holders.setOutcomingImageConfig(GWView.OutgoingDailyGWViewHolder.class, R.layout.item_feed_daily_gw);
        holders.registerContentType(
                (byte) GWMessageType,
                GWView.OutgoingImageViewHolder.class,
                R.layout.item_feed_daily_gw,
                GWView.OutgoingImageViewHolder.class,
                R.layout.item_feed_daily_gw,
                ChatSDKUI.shared().getMessageRegistrationManager());
    }

    @Override
    public boolean hasContentFor(MessageHolder holder) {
        return holder.message.typeIs(GWMessageType);
    }

    @Override
    public MessageHolder onNewMessageHolder(Message message) {
        if (message.typeIs(GWMessageType)) {
            return new ImageHolder(message);
        }
        return null;
    }

//    @Override
//    public boolean onClick(Activity activity, View rootView, Message message) {
//        if (message.typeIs(GWMessageType)) {
////            CardGenerator generator = CardGenerator.Companion.getInstance();
////            Bitmap bitmap = generator.getCacheBitmap(message.stringForKey(Keys.ImageUrl));
////            ImageMessageOnClickHandler.onClick(activity, rootView, bitmap);
//            Integer action = message.integerForKey("action");
//            if (action == GWThreadHandler.action_daily_gw) {
//                String dateStr = message.stringForKey("image-date");
//                if(dateStr==null||dateStr.isEmpty()){
//                    dateStr = DateLocalizationUtil.INSTANCE.formatDayAgo(0);
//                }
//                ImageViewerActivity.Companion.start(activity, dateStr);
//            } else {
//                ImageMessageOnClickHandler.onClick(activity, rootView, message.stringForKey(Keys.ImageUrl), message.stringForKey("image-text"));
//            }
//            return true;
//        } else {
//            return super.onClick(activity, rootView, message);
//        }
////        if (!super.onClick(activity, rootView, message)) {
////            if (message.typeIs(MessageType.Image)) {
////                ImageMessageOnClickHandler.onClick(activity, rootView, message.stringForKey(Keys.ImageUrl));
////                return true;
////            }
////            return false;
////        }
////        return true;
//    }
}
