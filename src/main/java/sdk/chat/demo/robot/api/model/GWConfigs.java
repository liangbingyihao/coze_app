package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class GWConfigs {
    private GWConfigItem configs;

    @SerializedName("default_msg")
    private MessageDetail defaultMsg;

    @SerializedName("text_to_speech_voices")
    private List<DBVoiceType> dbVoiceTypes;


    @SerializedName("tts_voice")
    private Map<String, String> defaultVoiceTypes;

    @SerializedName("stt_config")
    private VoiceBaseConfigs voiceBaseConfigs;

    public Map<String, String> getDefaultVoiceTypes() {
        return defaultVoiceTypes;
    }

    public void setDefaultVoiceTypes(Map<String, String> defaultVoiceTypes) {
        this.defaultVoiceTypes = defaultVoiceTypes;
    }

    public VoiceBaseConfigs getVoiceBaseConfigs() {
        return voiceBaseConfigs;
    }

    public void setVoiceBaseConfigs(VoiceBaseConfigs voiceBaseConfigs) {
        this.voiceBaseConfigs = voiceBaseConfigs;
    }

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

    public MessageDetail getDefaultMsg() {
        return defaultMsg;
    }

    public void setDefaultMsg(MessageDetail defaultMsg) {
        this.defaultMsg = defaultMsg;
    }

    public static class VoiceBaseConfigs{
        private String token;
        @SerializedName("app_id")
        private String appId;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
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
