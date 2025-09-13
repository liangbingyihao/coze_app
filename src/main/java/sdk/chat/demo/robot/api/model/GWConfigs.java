package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GWConfigs {
    private GWConfigItem configs;

//    @SerializedName("welcome_msg")
//    private MessageDetail welcomeMsg;

    @SerializedName("text_to_speech_voices")
    private List<DBVoiceType> dbVoiceTypes;
    public GWConfigItem getConfigs() {
        return configs;
    }

    public void setConfigs(GWConfigItem configs) {
        this.configs = configs;
    }

    public List<DBVoiceType> getDbVoiceTypes() {
        return dbVoiceTypes;
    }

    public void setDbVoiceTypes(List<DBVoiceType> dbVoiceTypes) {
        this.dbVoiceTypes = dbVoiceTypes;
    }

//    public MessageDetail getWelcomeMsg() {
//        return welcomeMsg;
//    }
//
//    public void setWelcomeMsg(MessageDetail welcomeMsg) {
//        this.welcomeMsg = welcomeMsg;
//    }

    public static class GWConfigItem {
        @SerializedName("generating_hint")
        private List<String> generatingHint;

        public List<String> getGeneratingHint() {
            return generatingHint;
        }

        public void setGeneratingHint(List<String> generatingHint) {
            this.generatingHint = generatingHint;
        }
    }


    public static class DBVoiceType {
        private String name;
        @SerializedName("voice_type")
        private String voiceType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVoiceType() {
            return voiceType;
        }

        public void setVoiceType(String voiceType) {
            this.voiceType = voiceType;
        }
    }
}
