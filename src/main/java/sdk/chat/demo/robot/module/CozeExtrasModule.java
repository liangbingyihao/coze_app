package sdk.chat.demo.robot.module;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.core.dao.Keys;
import sdk.chat.core.handlers.MessageHandler;
import sdk.chat.core.module.AbstractModule;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.session.Configure;
import sdk.chat.core.utils.ProfileOption;
import sdk.chat.demo.robot.activities.MainDrawerActivity;
import sdk.chat.demo.robot.handlers.CozeMsgHandler;
import sdk.chat.ui.extras.ScanQRCodeActivity;
import sdk.chat.ui.extras.ShowQRCodeActivity;
import sdk.guru.common.BaseConfig;
import sdk.guru.common.DisposableMap;


public class CozeExtrasModule extends AbstractModule {
    private static final CozeMsgHandler msgHandler = new CozeMsgHandler();

    public static final CozeExtrasModule instance = new CozeExtrasModule();

    public static CozeExtrasModule shared() {
        return instance;
    }

    public DisposableMap dm = new DisposableMap();

    /**
     * @see Config
     * @return configuration object
     */
    public static Config<CozeExtrasModule> builder() {
        return instance.config;
    }

    public static CozeExtrasModule builder(Configure<Config> config) throws Exception {
        config.with(instance.config);
        return instance;
    }

    public static class Config<T> extends BaseConfig<T> {

        public boolean drawerEnabled = true;
        public boolean qrCodesEnabled = true;

        /**
         * Default image drawer header area
         */
        @DrawableRes
        public int drawerHeaderImage = sdk.chat.ui.extras.R.drawable.header;

        public Config(T onBuild) {
            super(onBuild);
        }

        /**
         * Enable the navigation drawer
         * @param value
         * @return
         */
        public Config<T> setDrawerEnabled(boolean value) {
            drawerEnabled = value;
            return this;
        }

        /**
         * Set the default drawer header image
         * @param res
         * @return
         */
        public Config<T> setDrawerHeaderImage(@DrawableRes int res) {
            drawerHeaderImage = res;
            return this;
        }

        /**
         * Enable Qr code invitations
         * @param enabled
         * @return
         */
        public Config<T> setQrCodesEnabled(boolean enabled) {
            qrCodesEnabled = enabled;
            return this;
        }

    }

    protected Config<CozeExtrasModule> config = new Config<>(this);

    @Override
    public void activate(@Nullable Context context) {
        if (config.drawerEnabled) {
            ChatSDK.ui().setMainActivity(MainDrawerActivity.class);
        }
        if (config.qrCodesEnabled) {
            ChatSDK.ui().addSearchActivity(ScanQRCodeActivity.class, ChatSDK.getString(sdk.chat.ui.extras.R.string.qr_code));

            // Show the QR code when the user clicks the profile option
            ChatSDK.ui().addProfileOption(new ProfileOption(ChatSDK.getString(sdk.chat.ui.extras.R.string.qr_code), (activity, userEntityID) -> {

//                dm.add(ActivityResultPushSubjectHolder.shared()
//                        .doFinally(() -> dm.dispose())
//                        .subscribe(activityResult -> {
//
//
//
//                }));

                Intent intent = new Intent(activity, ShowQRCodeActivity.class);
                intent.putExtra(Keys.IntentKeyUserEntityID, userEntityID);
                activity.startActivity(intent);
            }));
        }
    }

    public static Config config() {
        return shared().config;
    }

    public List<String> requiredPermissions() {
        List<String> permissions= new ArrayList<>();
        if (config.qrCodesEnabled) {
            permissions.add(Manifest.permission.VIBRATE);
            permissions.add(Manifest.permission.CAMERA);
        }
        return permissions;
    }

    @Override
    public void stop() {
        config = new Config<>(this);
    }

    public boolean isPremium() {
        return false;
    }

    public MessageHandler getMessageHandler() {
        return msgHandler;
    }

}
