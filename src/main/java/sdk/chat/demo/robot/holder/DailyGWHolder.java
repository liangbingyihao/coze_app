package sdk.chat.demo.robot.holder;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.Date;

import sdk.chat.demo.robot.api.model.ImageDaily;

public class DailyGWHolder implements IMessage {
    private ImageDaily imageDaily;
    private final int action;

    public DailyGWHolder(int action, ImageDaily imageDaily) {
        this.action = action;
        this.imageDaily = imageDaily;
    }

    public ImageDaily getImageDaily() {
        return imageDaily;
    }

    public void setImageDaily(ImageDaily imageDaily) {
        this.imageDaily = imageDaily;
    }

    public int getAction() {
        return action;
    }

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
