package sdk.chat.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import sdk.chat.contact.ContactBookModule;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.Device;
import sdk.chat.demo.robot.ChatSDKCoze;
import sdk.chat.demo.robot.handlers.GWAuthenticationHandler;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.guru.common.DisposableMap;
import sdk.guru.common.RX;

import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bytedance.speech.speechengine.SpeechEngineGenerator;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.concurrent.TimeUnit;

public class MainApp extends Application implements Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private static Context context;
    private final DisposableMap dm = new DisposableMap();
    private boolean isInitialized = false;
    private Activity currentActivity;

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
        registerActivityLifecycleCallbacks(this);
        Log.i("MainApp", "MainApp.onCreate");
        context = getApplicationContext();
        scheduleTokenUpdate();

        Log.i("MainApp", "MainApp.PrepareEnvironment");
        SpeechEngineGenerator.PrepareEnvironment(getApplicationContext(), this);

        Log.i("MainApp", "MainApp.ChatSDKCoze");
        try {
            // Setup Chat SDK
            boolean drawerEnabled = !Device.honor();
            ChatSDKCoze.quickStartWithEmail(this, drawerEnabled,
                    "",
                    ContactBookModule.shared()
            );

            dm.add(ChatSDK.auth().authenticate()
                    .observeOn(RX.main())
                    .doFinally(GWAuthenticationHandler::ensureDatabase)
                    .subscribe(
                            () -> {
                                Log.w("MainApp", "authenticate done");
                                isInitialized = true;
                            },
                            error -> { /* 错误处理 */
                                Log.w("MainApp", error.getMessage());
                                isInitialized = true;
                            }
                    ));
        } catch (Exception e) {
            Log.i("MainApp", "MainApp.onCreate:e:"+e.getMessage());
            assert (false);
        }
        setupEnhancedCrashReporting();
    }

    private void setupEnhancedCrashReporting() {
        Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

                // 添加上下文信息
                crashlytics.setCustomKey("process",getApplicationInfo().processName);
                crashlytics.setCustomKey("last_activity",
                        currentActivity != null ? currentActivity.getClass().getSimpleName() : "none");
                crashlytics.setCustomKey("app_version",
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName);

                // 记录异常
                crashlytics.recordException(ex);
                crashlytics.sendUnsentReports();

            } catch (Exception e) {
                Log.e("CRASH_REPORT", "Error in crash handler", e);
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                } else {
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        currentActivity = null;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }


    public boolean isInitialized() {
        return isInitialized;
    }
}
