package sdk.chat.demo.robot.extensions;

import android.content.Context;
import android.util.Log;

import org.tinylog.configuration.Configuration;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class TinyLoggerManager {

    private static final String LOG_DIR = "logs";
    private static final long MAX_LOG_AGE_HOURS = 24; // 日志最大保留时间
    private static final long UPLOAD_WINDOW_MINUTES = 30; // 上报时间窗口

    public static void initialize(Context context) {
        try {
            File logDir = new File(context.getExternalFilesDir(null), LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 主日志（滚动）
//            Configuration.set("writer", "rolling file");
//            Configuration.set("writer.file", new File(logDir, "log_{count}.log").getAbsolutePath());
//            Configuration.set("writer.level", "debug");
//            Configuration.set("writer.format", "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {level}: {message}");
//            Configuration.set("writer.rolling.strategy", "size");
//            Configuration.set("writer.rolling.size", "10MB");
//            Configuration.set("writer.rolling.backups", "5");
//            Configuration.set("writer.policies", "startup, daily: 02:00");

            // 错误日志（也启用滚动）
            Configuration.set("writer2", "rolling file"); // 关键修改
            Configuration.set("writer2.file", new File(logDir, "error_{count}.log").getAbsolutePath());
            Configuration.set("writer2.level", "info");
            Configuration.set("writer.format", "{date:yyyy-MM-dd HH:mm:ss} [PID:{pid}] [{thread}] [{method}] {level}: {message}");
            Configuration.set("writer2.rolling.strategy", "size");
            Configuration.set("writer2.rolling.size", "10MB"); // 错误日志通常较小
            Configuration.set("writer2.rolling.backups", "3");
            Configuration.set("writer2.policies", "startup, daily: 02:00");
            Configuration.set("writer2.append", "true"); // 显式追加

            // 异步写入
            Configuration.set("writer.async", "true");
            Configuration.set("writer.async.capacity", "1000");

            Disposable disposable = Completable.fromAction(() -> cleanupOldLogs(logDir))
                    .subscribeOn(Schedulers.io())
                    .subscribeWith(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
//                            Log.d("LogManager", "日志清理完成");
                        }

                        @Override
                        public void onError(Throwable e) {
//                            Log.e("LogManager", "日志清理失败", e);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("LogManager", "Failed to initialize tinylog", e);
        }
    }

    // 清理过期日志
    private static void cleanupOldLogs(File logDir) {
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(MAX_LOG_AGE_HOURS);
                for (File file : files) {
                    if (file.lastModified() < cutoff) {
                        android.util.Log.e("LogManager", "del " + file.getName());
                        file.delete();
                    } else {
                        android.util.Log.e("LogManager", "skip: " + file.getName() + "," + file.lastModified());
                    }
                }
            }
        }
    }
}