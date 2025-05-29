package sdk.chat.demo.robot.adpter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.core.dao.Message;

public class AIExplore {
    private Message message;
    private List<ExploreItem> itemList;

    public AIExplore(Message message, List<ExploreItem> itemList) {
        this.message = message;
        this.itemList = itemList;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<ExploreItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<ExploreItem> itemList) {
        this.itemList = itemList;
    }

    public static class ExploreItem {
        private String text;
        private int action;
        private String params;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getAction() {
            return action;
        }

        public void setAction(int action) {
            this.action = action;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }

        public static ExploreItem loads(String exploreStr) {
            try {
                ExploreItem data = new ExploreItem();
                JsonArray items = JsonParser.parseString(exploreStr).getAsJsonArray();
                int len = items.size();
                if (len > 0) {
                    data.setText(items.get(0).getAsString());
                }
                if (len > 1) {
                    data.setAction(items.get(1).getAsInt());
                }
                if (len > 2) {
                    data.setParams(items.get(2).getAsString());
                }
                return data;
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    public static AIExplore loads(Message message) {
        List<ExploreItem> itemList = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            String exploreStr = message.stringForKey("explore_" + i);
            if (exploreStr != null) {
                ExploreItem d = ExploreItem.loads(exploreStr);
                if (d != null) {
                    itemList.add(d);
                }
            }
        }
        if (itemList.isEmpty()) {
            return null;
        }
        return new AIExplore(message, itemList);
    }
}
