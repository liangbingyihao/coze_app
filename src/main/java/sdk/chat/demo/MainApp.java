package sdk.chat.demo;

import android.app.Application;
import sdk.chat.contact.ContactBookModule;
import sdk.chat.core.utils.Device;
import sdk.chat.demo.robot.ChatSDKCoze;
import sdk.chat.message.audio.AudioMessageModule;
import sdk.chat.demo.robot.extensions.DeviceIdHelper;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Setup Chat SDK
            boolean drawerEnabled = !Device.honor();
            ChatSDKCoze.quickStartWithEmail(this, "pre_998", "AIzaSyCwwtZrlY9Rl8paM0R6iDNBEit_iexQ1aE",
                    drawerEnabled,
                    "",
                    AudioMessageModule.shared(),
//                    FirebaseBlockingModule.shared(),
//                    FirebaseReadReceiptsModule.shared(),
//                    FirebaseLastOnlineModule.shared(),
                    ContactBookModule.shared()
            );
//            ChatSDKFirebase.quickStartWithEmail(this, "pre_998", "AIzaSyCwwtZrlY9Rl8paM0R6iDNBEit_iexQ1aE", !Device.honor(), "team@sdk.chat",
//                    AudioMessageModule.shared(),
//                    FirebaseBlockingModule.shared(),
//                    FirebaseReadReceiptsModule.shared(),
//                    FirebaseLastOnlineModule.shared(),
//                    ContactBookModule.shared()
//            );

//            ChatSDK.events().sourceOnMain().subscribe(event -> {
//                Logger.debug(event);
//            });
//
//            ChatSDK.events().errorSourceOnMain().subscribe(event -> {
//                Logger.debug(event);
//                event.printStackTrace();
//            });

        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
    }
}
