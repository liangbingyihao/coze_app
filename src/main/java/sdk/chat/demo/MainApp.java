package sdk.chat.demo;

import android.app.Application;
import android.content.Context;

import sdk.chat.contact.ContactBookModule;
import sdk.chat.core.utils.Device;
import sdk.chat.demo.robot.ChatSDKCoze;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.chat.message.audio.AudioMessageModule;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainApp extends Application implements Configuration.Provider{
    private static Context context;

    public static Context getContext() {
        return context;
    }

    private void scheduleTokenUpdate() {
        // 创建每7天执行一次的定期工作请求
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                UpdateTokenWorker.class,
                7, // 重复间隔
                TimeUnit.DAYS)
                .addTag("FCM_TOKEN_UPDATE")
                .build();

        // 使用唯一工作名称确保只有一个实例运行
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "FCM_TOKEN_UPDATE_WORK",
                ExistingPeriodicWorkPolicy.KEEP, // 如果已有相同工作则保留
                workRequest);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        scheduleTokenUpdate();

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
