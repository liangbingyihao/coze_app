package sdk.chat.demo.robot;

import android.content.Context;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Arrays;
import java.util.List;

import sdk.chat.core.module.ImageMessageModule;
import sdk.chat.core.module.Module;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.QuickStart;
import sdk.chat.demo.robot.handlers.CozeNetworkAdapter;
import sdk.chat.demo.robot.module.CozeModule;
import sdk.chat.firebase.adapter.module.FirebaseModule;
import sdk.chat.firebase.push.FirebasePushModule;
import sdk.chat.firebase.ui.FirebaseUIModule;
import sdk.chat.firebase.upload.FirebaseUploadModule;
import sdk.chat.ui.extras.ExtrasModule;
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
//                        .setPublicRoomCreationEnabled(true)
//                        .setPublicRoomsEnabled(true)
                        .build(),

                FirebaseUploadModule.shared(),

//                LocationMessageModule.shared(),
//                ImageMessageModule.shared(),

                FirebasePushModule.shared(),

                ExtrasModule.builder(config -> {
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

    }

}
