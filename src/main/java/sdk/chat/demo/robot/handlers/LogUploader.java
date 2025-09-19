package sdk.chat.demo.robot.handlers;

import static sdk.chat.demo.robot.api.model.LogRequestKt.createBatchLogsRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.ImageApi;
import sdk.chat.demo.robot.api.model.KeyValuePair;
import sdk.chat.demo.robot.api.model.LogEntry;
import sdk.chat.demo.robot.api.model.LogRequest;

public class LogUploader {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static String URL_EVENT = ImageApi.URL2_MAIN + "log/event";


    private static long fastParseLogTime(String timeStr) {
        // 直接解析数字，避免 SimpleDateFormat 的开销
        int year  = Integer.parseInt(timeStr.substring(0, 4));
        int month = Integer.parseInt(timeStr.substring(5, 7));
        int day   = Integer.parseInt(timeStr.substring(8, 10));
        int hour  = Integer.parseInt(timeStr.substring(11, 13));
        int min   = Integer.parseInt(timeStr.substring(14, 16));
        int sec   = Integer.parseInt(timeStr.substring(17, 19));

        // 转换为时间戳（简化版，不考虑时区和夏令时）
        return (year - 1970) * 31_536_000_000L
                + (month - 1) * 2_628_000_000L
                + (day - 1) * 86_400_000L
                + hour * 3_600_000L
                + min * 60_000L
                + sec * 1_000L;
    }

    private static Long parseLogTime(String logLine) {
        try {
            // 假设日志格式: "2024-01-15 14:30:25 [main] ERROR: Something went wrong"
            if (logLine.length() >= 19) {
                String timeStr = logLine.substring(0, 19);
                // 这里需要根据实际格式解析时间
                // 可以使用 SimpleDateFormat 或 DateTimeFormatter
                // 简化示例，实际使用时需要完整实现
                return System.currentTimeMillis(); // 伪代码
            }
        } catch (Exception e) {
            // 解析失败
        }
        return null;
    }

    private static List<String> readLogsSince(File logFile, long sinceTime) {
        List<String> relevantLogs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (isLogAfterCutoff(line, sinceTime)) {
                        relevantLogs.add(line);
                    }else{
                        Log.e("LogManager", logFile.getName()+",skip log.."+line);
                    }
                } catch (Exception e) {
                    // 解析失败，保守处理为包含该日志
                    relevantLogs.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return relevantLogs;
    }

    private static boolean isLogAfterCutoff(String logLine, long cutoffTime) {
        // 简单检查是否可能包含时间戳
        if (logLine.length() < 20) {
            return true; // 不符合格式的日志行，保守处理为包含
        }

        try {
            String timeStr = logLine.substring(1, 19);
            long logTime = fastParseLogTime(timeStr);
            return logTime >= cutoffTime;
        } catch (Exception e) {
            return true; // 解析失败，保守处理为包含
        }
    }

    // 获取最近日志的 Single
    public static Single<List<String>> getRecentLogsForUpload(Context context) {
        return Single.fromCallable(() -> {
            List<String> logs = new ArrayList<>();
            File logDir = new File(context.getExternalFilesDir(null), "logs");

            if (logDir.exists()) {
                long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12);

                // 遍历所有 error_{count}.log 文件
                File[] errorLogFiles = logDir.listFiles((dir, name) ->
                        name.matches("error_\\d+\\.log") || name.equals("error.log")
                );
                if(errorLogFiles!=null){
                    Arrays.sort(errorLogFiles, Comparator.comparingLong(File::lastModified));
                    for (File logFile : errorLogFiles) {
                        if (logFile.lastModified() >= cutoffTime) {
                            logs.addAll(readLogsSince(logFile, cutoffTime));
                            Log.e("LogManager", logFile.getName() + ",after readLogsSince:" + logs.size());
                        } else {
                            Log.e("LogManager", logFile.getName() + ",skip file..");
                        }
                    }
                }

//                // 读取 error.log
//                File errorLog = new File(logDir, "error.log");
//                if (errorLog.exists() && errorLog.lastModified() >= cutoffTime) {
//                    logs.addAll(readLogsSince(errorLog, cutoffTime));
//                }
//
//                // 读取最新的 app.log
//                File appLog = new File(logDir, "app.log");
//                if (appLog.exists() && appLog.lastModified() >= cutoffTime) {
//                    logs.addAll(readLogsSince(appLog, cutoffTime));
//                }
            }

            return logs;
        }).subscribeOn(Schedulers.io());
    }


    public static Single<Boolean> uploadLogs(Context context, String topic,String desc) {
        return getRecentLogsForUpload(context)
                .flatMap(logs -> {
                    if (logs.isEmpty()) {
                        return Single.just(true); // 没有日志也算成功
                    }

                    String uid = "";
                    try {
                        uid = ChatSDK.currentUserID();
                    }catch (Exception ignored){

                    }
//
//                    String logData = String.join("\n", logs);
//                    List<KeyValuePair> kvPairs1 = new ArrayList<>();
//                    kvPairs1.add(new KeyValuePair("logData", logData));
//                    logRequest.getLogs().add(new LogEntry(System.currentTimeMillis()/1000, kvPairs1));

                    Gson gson = new Gson();
                    String json = gson.toJson(createBatchLogsRequest(topic,desc,uid,String.join("\n", logs)));

                    // 创建请求体
                    RequestBody body = RequestBody.create(json, JSON);

                    // 构建请求
                    Request request = new Request.Builder()
                            .url(URL_EVENT)
                            .post(body)
                            .header("Content-Type", "application/json")
                            .build();

                    // 异步执行请求
                    OkHttpClient client = GWApiManager.shared().getClient().newBuilder()
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build();

                    return Single.create(emitter -> {
                        try (Response response = client.newCall(request).execute()) {
                            if (response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                String data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("code").getAsString();
                                if("OK".equals(data)){
                                    emitter.onSuccess(true);
                                }else{
                                    emitter.onError(new Exception(data));
                                }
                            } else {
                                emitter.onSuccess(false);
                            }
                        } catch (IOException e) {
                            emitter.onError(e);
                        }
                    });

//                    client.newCall(request).enqueue(new Callback() {
//                        @Override
//                        public void onFailure(Call call, IOException e) {
//                            emitter.onError(e); // 请求失败
//                        }
//
//                        @Override
//                        public void onResponse(Call call, Response response) throws IOException {
//                            try {
//                                String responseBody = response.body() != null ? response.body().string() : "";
//                                String data = gson.fromJson(responseBody, JsonObject.class).getAsJsonPrimitive("code").getAsString();
//                                emitter.onSuccess("OK".equals(data) || "DUPLICATE_OPERATION".equals(data)); // 请求成功
//                            } catch (Exception e) {
//                                emitter.onError(e);
//                            } finally {
//                                response.close(); // 关闭 Response
//                            }
//                        }
//                    });
                })
                .subscribeOn(Schedulers.io());
    }


    // 简单的 Pair 实现
    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }
}
