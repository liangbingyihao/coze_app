package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

public class ExportInfo {
    @SerializedName("download_url")
    private String downLoadUrl;
    @SerializedName("expires_in")
    private Integer expireIn;

    public String getDownLoadUrl() {
        return downLoadUrl;
    }

    public void setDownLoadUrl(String downLoadUrl) {
        this.downLoadUrl = downLoadUrl;
    }

    public Integer getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(Integer expireIn) {
        this.expireIn = expireIn;
    }
}
