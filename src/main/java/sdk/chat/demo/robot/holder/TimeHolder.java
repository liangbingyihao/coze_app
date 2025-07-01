package sdk.chat.demo.robot.holder;


import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import java.util.Date;

import kotlin.jvm.JvmField;
import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.handlers.GWMsgHandler;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.ui.chat.model.MessageHolder;

public class TimeHolder implements IMessage {


    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public String getPreview() {
        return "";
    }

    @Override
    public IUser getUser() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return null;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void makeClean() {

    }
}
