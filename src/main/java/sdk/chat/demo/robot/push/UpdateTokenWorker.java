package sdk.chat.demo.robot.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.ExecutionException;

public class UpdateTokenWorker extends Worker {

    public UpdateTokenWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 获取新的 FCM Token
            String token = Tasks.await(FirebaseMessaging.getInstance().getToken());

            // 存储 Token 到服务器
            boolean storedSuccessfully = storeTokenToServer(token);

            if (storedSuccessfully) {
                // 可选：将 Token 存储在本地 SharedPreferences
                saveTokenLocally(token);
                return Result.success();
            } else {
                return Result.retry(); // 存储失败时重试
            }
        } catch (ExecutionException | InterruptedException e) {
            return Result.retry(); // 获取 Token 失败时重试
        } catch (Exception e) {
            return Result.failure(); // 其他不可恢复的错误
        }
    }

    private boolean storeTokenToServer(String token) {
        // 实现将 Token 发送到您的服务器的逻辑
        // 这里应该是您的网络请求代码
        try {
            // 示例伪代码：
            // YourApiClient client = RetrofitClient.getClient();
            // Response response = client.updateFcmToken(getUserId(), getDeviceId(), token).execute();
            // return response.isSuccessful();

            return true; // 假设总是成功
        } catch (Exception e) {
            return false;
        }
    }

    private void saveTokenLocally(String token) {
        getApplicationContext().getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                .edit()
                .putString("FCM_TOKEN", token)
                .apply();
    }

    private String getUserId() {
        // 从本地存储获取用户 ID
        // 实现根据您的应用逻辑
        return "";
    }

    private String getDeviceId() {
        // 获取设备唯一标识
        // 实现根据您的应用逻辑
        return "";
    }

    public static String checkAndUpdateToken(Context context) {
        // 检查本地是否已存储 Token
        String savedToken = context.getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                .getString("FCM_TOKEN", null);

        if (savedToken == null) {
            // 立即执行一次 Token 更新
            OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(UpdateTokenWorker.class)
                    .build();
            WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }
        return savedToken;
    }

    public static void forceUpdateToken(Context context) {
        // 强制立即更新 Token
        OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(UpdateTokenWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(oneTimeRequest);
    }
}