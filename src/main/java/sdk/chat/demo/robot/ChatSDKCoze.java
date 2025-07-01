package sdk.chat.demo.robot;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import sdk.chat.core.module.ImageMessageModule;
import sdk.chat.core.module.Module;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.QuickStart;
import sdk.chat.demo.examples.message.ExampleSnapMessageRegistration;
import sdk.chat.demo.robot.holder.DailyGWRegistration;
import sdk.chat.demo.robot.module.CozeModule;
import sdk.chat.demo.robot.module.GWExtrasModule;
import sdk.chat.demo.robot.holder.ImageRegistration;
import sdk.chat.demo.robot.holder.TextRegistration;
import sdk.chat.firebase.push.FirebasePushModule;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.module.UIModule;

public class ChatSDKCoze  extends QuickStart {
    public static void quickStartWithEmail(Context context,  boolean drawerEnabled, String email, Module... modules) throws Exception {
        quickStart(context,  drawerEnabled, email(email), modules);
    }


    /**
     * @param context
     * @param drawerEnabled Whether to use drawer or tabs (Default)
     * @param modules Optional modules
     * @throws Exception
     */
    public static void quickStart(Context context,  boolean drawerEnabled, String identifier, Module... modules) throws Exception {

        List<Module> newModules = Arrays.asList(
                CozeModule.builder()
                        .build(),

                UIModule.builder()
                        .setLocationMessagesEnabled(false)
                        .setPublicRoomsEnabled(false)
                        .setMessageSelectionEnabled(false)
//                        .setPublicRoomsEnabled(true)
                        .build(),

//                FirebaseUploadModule.shared(),

//                LocationMessageModule.shared(),
                ImageMessageModule.shared(),

                FirebasePushModule.shared(),

                GWExtrasModule.builder(config -> {
                    config.setDrawerEnabled(drawerEnabled);
                })

//                FirebaseUIModule.builder()
//                        .setProviders(EmailAuthProvider.PROVIDER_ID, PhoneAuthProvider.PROVIDER_ID)
//                        .build()
        );

        ChatSDK.builder()
//                .setGoogleMaps(googleMapsKey)
                .setAnonymousLoginEnabled(false)
                .setRemoteConfigEnabled(false)
//                .setPublicChatRoomLifetimeMinutes(TimeUnit.HOURS.toMinutes(24))
                .setSendSystemMessageWhenRoleChanges(false)
                .build()
                .addModules(deduplicate(newModules, modules))
                // Activate
                .build()
                .activate(context, identifier);
//        ChatSDKUI.setPrivateThreadsFragment(new CustomPrivateThreadsFragment());
        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new TextRegistration());
        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new ImageRegistration());
        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new DailyGWRegistration());

    }

}
