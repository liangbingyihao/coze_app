package sdk.chat.demo.robot.adpter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.core.dao.Message;

public class AIExplore {
    private Message message;
    private List<ExploreItem> itemList;

    private String contextId;

    public AIExplore(Message message, List<ExploreItem> itemList) {
        this.message = message;
        this.itemList = itemList;
        this.contextId = null;
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

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
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

        public static ExploreItem loads(List<String> func) {
            try {
                ExploreItem data = new ExploreItem();
                int len = func.size();
                if (len > 0) {
                    data.setText(func.get(0));
                }
                if (len > 1) {
                    data.setAction(Integer.parseInt(func.get(1)));
                }
                if (len > 2) {
                    data.setParams(func.get(2));
                }
                return data;
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    public static AIExplore loads(Message message, List<List<String>> functions) {
        if(functions==null||functions.isEmpty()){
            return null;
        }
        List<ExploreItem> itemList = new ArrayList<>();
        for (List<String> func : functions) {
            ExploreItem d = ExploreItem.loads(func);
            if (d != null) {
                itemList.add(d);
            }
        }
        if (itemList.isEmpty()) {
            return null;
        }
        return new AIExplore(message, itemList);
    }
}
