package sdk.chat.demo.robot.holder;

import android.view.View;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessageHolders;


public class IncomingTextViewHolder<MESSAGE extends IMessage> extends MessageHolders.BaseIncomingMessageViewHolder<MESSAGE> {

    public IncomingTextViewHolder(View itemView) {
        super(itemView);
    }

    public IncomingTextViewHolder(View itemView, Object payload) {
        super(itemView, payload);
    }
}
