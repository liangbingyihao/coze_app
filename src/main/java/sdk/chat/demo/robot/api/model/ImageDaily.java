package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.demo.robot.handlers.GWThreadHandler;

public class ImageDaily {
    private String scripture;
    private String url;
    private String date;
    private String reference;
    @SerializedName("background_url")
    private String backgroundUrl;
    private ArrayList<String> explore;
//    private ArrayList<String> prompt;

    public ImageDaily(String scripture, String backgroundUrl) {
        this.scripture = scripture;
        this.backgroundUrl = backgroundUrl;
    }

    public String getScripture() {
        return scripture;
    }

    public void setScripture(String scripture) {
        this.scripture = scripture;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    public ArrayList<String> getExplore() {
        return explore;
    }

    public void setExplore(ArrayList<String> explore) {
        this.explore = explore;
    }

    public List<List<String>> getExploreWithParams() {

        List<List<String>> function = new ArrayList<>();
//        for(String f:prompt){
//            function.add(List.of(f, Integer.toString(GWThreadHandler.action_input_prompt), f));
//        }
        for (String f : explore) {
            function.add(
                    List.of(f)
            );
//            if (f.indexOf("祷告默想和实际应用") > 0) {
//                function.add(
//                        List.of(f, Integer.toString(GWThreadHandler.action_daily_pray), scripture)
//                );
//            } else {
//                function.add(
//                        List.of(f)
//                );
//            }
        }

        return function;
    }

//    public ArrayList<String> getPrompt() {
//        return prompt;
//    }
//
//    public void setPrompt(ArrayList<String> prompt) {
//        this.prompt = prompt;
//    }
}
