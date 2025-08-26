package sdk.chat.demo.robot.handlers;

import static sdk.chat.demo.robot.api.GWApiManager.buildPostRequest;
import static sdk.chat.demo.robot.api.model.TaskDetail.UNLOCK_STORY_MASK;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.JsonCacheManager;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.api.model.ImageDailyList;
import sdk.chat.demo.robot.api.model.TaskDetail;
import sdk.chat.demo.robot.api.model.TaskHistory;
import sdk.chat.demo.robot.api.model.TaskProgress;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;

public class DailyTaskHandler {
    private final static Gson gson = new Gson();
    private final static String KEY_CACHE_TASK_DETAIL = "gwTaskDetail";
    private final static String KEY_CACHE_TASK_PROCESS = "gwTaskProcess";
    private final static String KEY_CACHE_TASK_HISTORY = "gwTaskHistory";
    private final static int MAX_TASK_INDEX = 6;
    private final static String URL_UNLOCK_STORY = ImageApi.URL2 + "story/unlock";
    private final static String URL_STORY_PROGRESS = ImageApi.URL2 + "story/progress";
    private final static String URL_STORY_HISTORY = ImageApi.URL2 + "story/history";
    private final static String timeZoneId = TimeZone.getDefault().getID();

    public static void testTaskDetail(Integer completeIndex) {
        String today = DateLocalizationUtil.INSTANCE.formatDayAgo(0);
        TaskDetail item = new TaskDetail(today);
        if (completeIndex > MAX_TASK_INDEX) {
            item.setIndex(MAX_TASK_INDEX);
            item.setStatus(0b111);
        } else if (completeIndex < 0) {
            item.setIndex(0);
        } else {
            item.setIndex(completeIndex);
        }
        JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_DETAIL, gson.toJson(item));
    }

    public static void completeTaskByIndex(int index){
        TaskDetail taskDetail = getTaskToday();
        taskDetail.completeTaskByIndex(index);
        setTaskDetail(taskDetail);
    }

    public static void setTaskDetail(TaskDetail detail) {
        if (detail == null) {
            Log.e("TaskManager", "TaskDetail cannot be null");
            return;
        }

        JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_DETAIL, gson.toJson(detail));
        if (!detail.isTaskCompleted(UNLOCK_STORY_MASK)&&detail.isAllUserTaskCompleted()) {
            unlockStory()
                    .subscribeOn(Schedulers.io()) // 在IO线程执行网络请求
                    .observeOn(AndroidSchedulers.mainThread()) // 在主线程处理结果
                    .subscribe(new SingleObserver<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            // 可以在这里保存Disposable以便后续管理
                            Log.d("TaskManager", "Start unlocking story");
                        }

                        @Override
                        public void onSuccess(Boolean isSuccess) {
                            if (isSuccess) {
                                // 3. 网络请求成功，保存到数据库
                                detail.setTaskCompleted(UNLOCK_STORY_MASK, true);
                                JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_DETAIL, gson.toJson(detail));
                            } else {
                                Log.e("TaskManager", "Failed to unlock story");
                                // 可以在这里添加失败处理逻辑
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("TaskManager", "Error unlocking story: " + e.getMessage());
                        }
                    });
        }
    }


    public static TaskDetail getTaskToday() {
        String cachedData = JsonCacheManager.INSTANCE.get(MainApp.getContext(), KEY_CACHE_TASK_DETAIL);
        String today = DateLocalizationUtil.INSTANCE.formatDayAgo(0);
        if (cachedData != null) {
            TaskDetail task = gson.fromJson(cachedData, TaskDetail.class);
            //四种可能:以前已完成，以前未完成，今天已完成、今天未完成,(异常的时间：将来，也归为以前)
            if (!today.equals(task.getTaskDate())) {
                task.setTaskDate(today);
                task.setStatus(0);
                if (task.isAllCompleted()) {
                    //历史没打完卡的，今天继续沿用最后一次的索引
                    int newIndex = task.getIndex() >= MAX_TASK_INDEX ? 0 : task.getIndex();
                    task.setIndex(newIndex);
                }
            }
            return task;
        } else {
            return new TaskDetail(today);
        }
    }

    public static Single<TaskProgress> getTaskProgress() {
        return Single.create(emitter -> {
            TaskDetail taskToday = getTaskToday();
            String cachedData = JsonCacheManager.INSTANCE.get(MainApp.getContext(), KEY_CACHE_TASK_PROCESS);
            if (cachedData != null&&!cachedData.isEmpty()) {
                TaskProgress progress = gson.fromJson(cachedData, TaskProgress.class);
                if (progress!=null&&taskToday.getCntComplete() == progress.getUnlocked()
                        &&!progress.getProgressImage().isEmpty()
                        &&(progress.getUnlocked()==0||progress.getUnlocked()==progress.getChapters().size())) {
                    progress.setTaskDetail(taskToday);
                    emitter.onSuccess(progress);
                    return;
                }
            }
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_STORY_PROGRESS))
                    .newBuilder()
                    .addQueryParameter("tz", timeZoneId)
                    .addQueryParameter("lang", Locale.getDefault().toString())
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = GWApiManager.shared().getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new IOException("HTTP error: " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                TaskProgress progress = gson.fromJson(data, TaskProgress.class);
                if (progress != null) {
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_PROCESS, data.toString());
                    taskToday.setIndexByCntComplete(progress.getUnlocked());
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_DETAIL, gson.toJson(taskToday));
                    progress.setTaskDetail(taskToday);
                }
                emitter.onSuccess(progress);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Single<Boolean> unlockStory() {
        return Single.create(emitter -> {
            Map<String, String> params = new HashMap<>();
            params.put("tz", timeZoneId);

            Request request = buildPostRequest(params, URL_UNLOCK_STORY);

            OkHttpClient client = GWApiManager.shared().getClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
//                        if (!response.isSuccessful()) {
//                            emitter.onError(new IOException("HTTP error: " + response.code()));
//                            return;
//                        }
                        JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_HISTORY,"");
                        String responseBody = response.body() != null ? response.body().string() : "";
                        String data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("code").getAsString();
                        emitter.onSuccess("OK".equals(data) || "DUPLICATE_OPERATION".equals(data)); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public static Single<TaskHistory> getTaskHistory() {
        return Single.create(emitter -> {
            TaskDetail taskToday = getTaskToday();
            String cachedData = JsonCacheManager.INSTANCE.get(MainApp.getContext(), KEY_CACHE_TASK_HISTORY);
            if (cachedData != null&&!cachedData.isEmpty()) {
                TaskHistory progress = gson.fromJson(cachedData, TaskHistory.class);
                if (progress!=null) {
                    emitter.onSuccess(progress);
                }
            }
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_STORY_HISTORY))
                    .newBuilder()
                    .addQueryParameter("start_month", "2025-07")
                    .addQueryParameter("lang", Locale.getDefault().toString())
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = GWApiManager.shared().getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new IOException("HTTP error: " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                TaskHistory progress = gson.fromJson(data, TaskHistory.class);
                if (progress != null) {
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_TASK_HISTORY, data.toString());
                    emitter.onSuccess(progress);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
