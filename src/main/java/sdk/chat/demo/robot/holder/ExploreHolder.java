package sdk.chat.demo.robot.holder;


import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.Date;


public class ExploreHolder implements IMessage {


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
