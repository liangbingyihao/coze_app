package sdk.chat.demo.robot;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import sdk.chat.core.module.Module;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.QuickStart;
import sdk.chat.demo.examples.helper.CustomPrivateThreadsFragment;
import sdk.chat.demo.examples.message.DefaultTextMessageRegistration;
import sdk.chat.demo.examples.message.ExampleSnapMessageRegistration;
import sdk.chat.demo.robot.module.GWExtrasModule;
import sdk.chat.demo.robot.module.CozeModule;
import sdk.chat.demo.robot.ui.GWMessageRegistration;
import sdk.chat.firebase.push.FirebasePushModule;
//import sdk.chat.firebase.upload.FirebaseUploadModule;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.module.UIModule;

public class ChatSDKCoze  extends QuickStart {
    public static void quickStartWithEmail(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String email, Module... modules) throws Exception {
        quickStart(context, rootPath, googleMapsKey, drawerEnabled, email(email), modules);
    }


    /**
     * @param context
     * @param rootPath Firebase base path (can be any string, cannot contain special characters)
     * @param googleMapsKey Google static maps key.
     * @param drawerEnabled Whether to use drawer or tabs (Default)
     * @param modules Optional modules
     * @throws Exception
     */
    public static void quickStart(Context context, String rootPath, String googleMapsKey, boolean drawerEnabled, String identifier, Module... modules) throws Exception {

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
//                ImageMessageModule.shared(),

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
                .setSendSystemMessageWhenRoleChanges(true)
                .build()
                .addModules(deduplicate(newModules, modules))
                // Activate
                .build()
                .activate(context, identifier);
        ChatSDKUI.setPrivateThreadsFragment(new CustomPrivateThreadsFragment());
        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new GWMessageRegistration());

//        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new DefaultTextMessageRegistration());
//        ChatSDKUI.shared().getMessageRegistrationManager().addMessageRegistration(new ExampleSnapMessageRegistration());

    }

}
