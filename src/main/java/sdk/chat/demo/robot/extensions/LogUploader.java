package sdk.chat.demo.robot.extensions;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LogUploader {
    public static List<String> getRecentLogsForUpload(Context context) {
        List<String> logs = new ArrayList<>();
        File logDir = new File(context.getExternalFilesDir(null), "logs");

        if (logDir.exists()) {
            long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);

            // 读取 error.log
            File errorLog = new File(logDir, "error.log");
            if (errorLog.exists() && errorLog.lastModified() >= cutoffTime) {
                logs.addAll(readLogsSince(errorLog, cutoffTime));
            }

            // 读取最新的 app.log
            File appLog = new File(logDir, "app.log");
            if (appLog.exists() && appLog.lastModified() >= cutoffTime) {
                logs.addAll(readLogsSince(appLog, cutoffTime));
            }
        }

        return logs;
    }

    private static List<String> readLogsSince(File logFile, long sinceTime) {
        List<String> relevantLogs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 解析日志时间（根据你的日志格式调整）
                Long logTime = parseLogTime(line);
                if (logTime != null && logTime >= sinceTime) {
                    relevantLogs.add(line);
                }
            }
        } catch (IOException e) {
            android.util.Log.e("LogUploader", "Failed to read log file", e);
        }

        return relevantLogs;
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

    public static void uploadLogs(Context context) {
        new Thread(() -> {
            try {
                List<String> logs = getRecentLogsForUpload(context);
                if (!logs.isEmpty()) {
                    // 这里实现你的上传逻辑
                    String logData = String.join("\n", logs);
                    // uploadToServer(logData);

                    android.util.Log.i("LogUploader", "Uploaded " + logs.size() + " log entries:"+logData);
                }
            } catch (Exception e) {
                android.util.Log.e("LogUploader", "Log upload failed", e);
            }
        }).start();
    }
}
