package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import org.pmw.tinylog.Logger;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import sdk.chat.core.dao.Message;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.manager.ImageMessagePayload;
import sdk.chat.core.manager.MessagePayload;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.Progress;
import sdk.chat.core.types.ReadStatus;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.R;
import sdk.chat.ui.chat.model.ImageMessageHolder;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.chat.model.UserHolder;
import sdk.chat.ui.module.UIModule;
import sdk.chat.ui.view_holders.v2.MessageDirection;
import sdk.guru.common.DisposableMap;

public class GWMessageHolder extends MessageHolder implements MessageContentType {

    public GWMessageHolder(Message message) {
        super(message);
    }

    public void updateNextAndPreviousMessages() {
        Message nextMessage = message.getNextMessage();
        Message previousMessage = message.getPreviousMessage();

        boolean isLast = nextMessage == null;
        if (isLast != this.isLast) {
            this.isLast = isLast;
            isDirty = true;
        }

        if (!isDirty) {
            String oldNextMessageId = this.nextMessage != null ? this.nextMessage.getEntityID() : "";
            String newNextMessageId = nextMessage != null ? nextMessage.getEntityID() : "";
            isDirty = !oldNextMessageId.equals(newNextMessageId);
        }

        if (!isDirty) {
            String oldPreviousMessageId = this.previousMessage != null ? this.previousMessage.getEntityID() : "";
            String newPreviousMessageId = previousMessage != null ? previousMessage.getEntityID() : "";
            isDirty = !oldPreviousMessageId.equals(newPreviousMessageId);
        }

        this.nextMessage = nextMessage;
        this.previousMessage = previousMessage;

        previousSenderEqualsSender = previousMessage != null && message.getSender().equalsEntity(previousMessage.getSender());
        nextSenderEqualsSender = nextMessage != null && message.getSender().equalsEntity(nextMessage.getSender());

        DateFormat format = UIModule.shared().getMessageBinder().messageTimeComparisonDateFormat(ChatSDK.ctx());
        showDate = nextMessage == null || !(format.format(message.getDate()).equals(format.format(nextMessage.getDate())) && nextSenderEqualsSender);
//        isGroup = message.getThread().typeIs(ThreadType.Group);

        Logger.warn("Message: " + message.getText() + ", showDate: " + showDate);
    }

    public void updateReadStatus() {
    }
}
