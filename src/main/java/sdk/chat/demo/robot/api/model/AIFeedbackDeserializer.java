package sdk.chat.demo.robot.api.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import com.google.gson.*;
import java.util.ArrayList;

import sdk.chat.demo.robot.adpter.data.AIExplore;

public class AIFeedbackDeserializer implements JsonDeserializer<AIFeedback> {

    @Override
    public AIFeedback deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();
        AIFeedback feedback = new AIFeedback();

//        // 处理 color_tag 字段
//        if (jsonObject.has("color_tag")) {
//            feedback.setColorTag(jsonObject.get("color_tag").getAsString());
//        }

        // 处理 functions 字段
        if (jsonObject.has("functions")) {
            Type listType = new TypeToken<List<AIExplore.ExploreItem>>(){}.getType();
            feedback.setFunctions(context.deserialize(jsonObject.get("functions"), listType));
        } else {
            feedback.setFunctions(new ArrayList<>()); // 默认空列表
        }

        // 处理 topic 字段
        if (jsonObject.has("topic")) {
            feedback.setTopic(jsonObject.get("topic").getAsString());
        }

        // 处理 tag 字段
        if (jsonObject.has("tag")) {
            feedback.setTag(jsonObject.get("tag").getAsString());
        }

        // 处理 bible 字段
        if (jsonObject.has("bible")) {
            feedback.setBible(jsonObject.get("bible").getAsString());
        }

        // 处理 view 字段
        if (jsonObject.has("view")) {
            feedback.setView(jsonObject.get("view").getAsString());
        }

        // 处理 explore 字段（兼容数组和字符串）
        if (jsonObject.has("explore")) {
            JsonElement exploreElement = jsonObject.get("explore");
            if (exploreElement.isJsonArray()) {
                // 如果是数组，直接解析
                Type exploreListType = new TypeToken<List<String>>(){}.getType();
                feedback.setExplore(context.deserialize(exploreElement, exploreListType));
            } else if (exploreElement.isJsonPrimitive()) {
                // 如果是字符串，转换为单元素列表
                String exploreStr = exploreElement.getAsString();
                if (!exploreStr.isEmpty()) {
                    feedback.setExplore(Collections.singletonList(exploreStr));
                } else {
                    feedback.setExplore(new ArrayList<>());
                }
            }
        } else {
            feedback.setExplore(new ArrayList<>()); // 默认空列表
        }

        // 处理 prompt 字段
        if (jsonObject.has("prompt")) {
            feedback.setPrompt(jsonObject.get("prompt").getAsString());
        }

        return feedback;
    }
}