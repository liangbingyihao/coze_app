package sdk.chat.demo.robot.api.model;

import java.util.List;

public class MessageList {
    private List<MessageDetail> items;

    public List<MessageDetail> getItems() {
        return items;
    }

    public void setItems(List<MessageDetail> items) {
        this.items = items;
    }
}
