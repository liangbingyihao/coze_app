package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class FavoriteList {
    private List<FavoriteItem> items;

    public List<FavoriteList.FavoriteItem> getItems() {
        return items;
    }

    public void setItems(List<FavoriteList.FavoriteItem> items) {
        this.items = items;
    }

    public static class FavoriteItem {
        @SerializedName("message_id")
        private String messageId;
        @SerializedName("content_type")
        private String contentType;
        @SerializedName("created_at")
        private String createdAt;
        private String content;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

