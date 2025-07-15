package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GWConfigs {
    private GWConfigItem configs;

    public GWConfigItem getConfigs() {
        return configs;
    }

    public void setConfigs(GWConfigItem configs) {
        this.configs = configs;
    }

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
}
