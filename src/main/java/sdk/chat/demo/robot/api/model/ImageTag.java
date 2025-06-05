package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ImageTag {
    private String name;
    @SerializedName("imgs")
    private List<ImageItem> images;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ImageItem> getImages() {
        return images;
    }

    public void setImages(List<ImageItem> images) {
        this.images = images;
    }
}
