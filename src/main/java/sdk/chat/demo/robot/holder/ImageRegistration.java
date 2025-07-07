package sdk.chat.demo.robot.holder;

import android.content.Context;

import com.stfalcon.chatkit.messages.MessageHolders;

import sdk.chat.core.dao.Message;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.custom.ImageMessageRegistration;

public class ImageRegistration extends ImageMessageRegistration {

    @Override
    public void onBindMessageHolders(Context context, MessageHolders holders) {
//        holders.setIncomingTextConfig(GW.IncomingMessageViewHolder.class, R.layout.item_incoming_text)
//                .setOutcomingTextConfig(GW.OutcomingMessageViewHolder.class, sdk.chat.ui.R.layout.view_holder_outcoming_text_message);
//        holders.setIncomingImageHolder(GWView.IncomingMessageViewHolder.class, R.layout.item_incoming_text);
        holders.setOutcomingImageConfig(GWView.OutgoingImageViewHolder.class, R.layout.item_feed_bible_pic);

    }

    @Override
    public MessageHolder onNewMessageHolder(Message message) {
        if (message.typeIs(MessageType.Image)) {
            return new ImageHolder(message);
        }
        return null;
    }
//
////    @Override
//    public boolean onClick1(Activity activity, View rootView, Message message) {
//        if (message.typeIs(MessageType.Image)) {
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
