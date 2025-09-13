package sdk.chat.demo.robot.adpter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.core.dao.Message;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.handlers.GWMsgHandler;

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
        //    action_daily_ai = 0
//    action_bible_pic = 1
//    action_daily_gw = 2
//    action_direct_msg = 3
//    action_daily_pray = 4
        public final static int action_bible_pic = 1;
        public final static int action_daily_gw = 2;
        public final static int action_direct_msg = 3;
        public final static int action_daily_pray = 4;
        public final static int action_input_prompt = 5;
        public final static int action_daily_gw_pray = 6;

        private String text;
        private int action;
        private List<String> params;

        public ExploreItem() {
        }

        public ExploreItem(int action, List<String> params, String text) {
            this.action = action;
            this.params = params;
            this.text = text;
        }

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

        public List<String> getParams() {
            return params;
        }

        public String getParamsStr() {
            //FIXME
            if (params == null || params.isEmpty()) {
                return null;
            }
            return params.get(0);
        }

        public void setParams(List<String> params) {
            this.params = params;
        }

//        public static ExploreItem loads(List<String> func) {
//            try {
//                ExploreItem data = new ExploreItem();
//                int len = func.size();
//                if (len > 0) {
//                    data.setText(func.get(0));
//                }
//                if (len > 1) {
//                    data.setAction(Integer.parseInt(func.get(1)));
//                }
//                if (len > 2) {
//                    data.setParams(func.get(2));
//                }
//                return data;
//            } catch (Exception ignored) {
//            }
//            return null;
//        }
    }

//    public static AIExplore loads1(Message message, List<List<String>> functions) {
//        if(functions==null||functions.isEmpty()){
//            return null;
//        }
//        List<ExploreItem> itemList = new ArrayList<>();
//        for (List<String> func : functions) {
//            ExploreItem d = ExploreItem.loads(func);
//            if (d != null) {
//                itemList.add(d);
//            }
//        }
//        if (itemList.isEmpty()) {
//            return null;
//        }
//        return new AIExplore(message, itemList);
//    }


    public static AIExplore loads(Message message) {
        MessageDetail messageDetail = GWMsgHandler.getAiFeedback(message);
        if (messageDetail == null || messageDetail.getFeedback() == null) {
            return null;
        }
        List<ExploreItem> itemList = messageDetail.getFeedback().getFunctions();
        if(itemList!=null&&!itemList.isEmpty()){
            return new AIExplore(message, itemList);
        }
        itemList = new ArrayList<>();
        List<String> explores = messageDetail.getFeedback().getExplore();
        if (explores != null && !explores.isEmpty()) {
            for (String e : explores) {
                itemList.add(new ExploreItem(0, null, e));
            }
        }
        String prompt = messageDetail.getFeedback().getPrompt();
        if (prompt != null && !prompt.isEmpty()) {
            itemList.add(new ExploreItem(ExploreItem.action_input_prompt, null, prompt));
        }
        if (itemList.isEmpty()) {
            return null;
        }
        return new AIExplore(message, itemList);
    }
}
