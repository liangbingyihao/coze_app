package sdk.chat.demo.robot.holder

import android.view.View
import sdk.chat.ui.chat.model.ImageMessageHolder
import sdk.chat.ui.chat.model.MessageHolder
import sdk.chat.ui.view_holders.v2.MessageDirection

class GWView {
    open class IncomingMessageViewHolder(itemView: View) :
        ChatTextViewHolder<MessageHolder>(itemView)

    open class OutgoingMessageViewHolder(itemView: View) :
        ChatTextViewHolder<MessageHolder>(itemView)

    open class OutgoingImageViewHolder(itemView: View) :
        ChatImageViewHolder<ImageMessageHolder>(itemView)

//    open class OutgoingDailyGWViewHolder(itemView: View) :
//        DailyGWViewHolder<ImageMessageHolder>(itemView, MessageDirection.Outcoming)
}