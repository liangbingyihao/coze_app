package sdk.chat.demo.robot.extensions

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.tinylog.Logger;

object LogHelper {
//    private const val LOG_CLEAR_INTERVAL = 120_000L // 2分钟（毫秒）
//    var logStr = ""
//        private set // 外部可读不可直接修改
//    private var lastLogTime = 0L

//    /**
//     * 添加带时间戳的日志
//     * @param newLog 日志内容（自动添加时间前缀）
//     */
//    fun appendLog(newLog: String) {
//        error(newLog)
//        val currentTime = System.currentTimeMillis()

//        // 超过间隔时间则清空旧日志
//        if (currentTime - lastLogTime > LOG_CLEAR_INTERVAL) {
//            logStr = ""
//        }
//
//        // 格式化时间戳 + 日志内容
//        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
//            .format(Date(currentTime))
//        logStr += "\n[$timestamp] $newLog"
//
//        lastLogTime = currentTime
//    }


    fun reportExportEvent(event: String, log: String, throwable: Throwable?) {
//        appendLog("<$event>:$log")
//        if (throwable != null) {
//            appendLog(throwable.toString())
//        }
        val crashlytics = FirebaseCrashlytics.getInstance()


        // 记录基本信息
        crashlytics.log(log)


        // 设置自定义键
        crashlytics.setCustomKey("event", event)


        // 如果有错误
        if (throwable != null) {
            crashlytics.recordException(throwable)
        }

    }

//    // 记录错误（会同时记录到 error.log 和 app.log）
//    fun error(message: String?, throwable: Throwable?) {
//        Logger.error(throwable, message)
//
//
//        // 额外记录到 Android Logcat（可选）
//        Log.e("AppError", message, throwable)
//    }
//
//    fun error(message: String) {
//        Logger.error(message as Any)
//        Log.e("AppError", message)
//    }
//
//    // 记录警告
//    fun warn(message: String) {
//        Logger.warn(message as Any)
//        Log.w("AppWarn", message)
//    }
//
//    // 记录信息
//    fun info(message: String) {
//        Logger.info(message as Any)
//        Log.i("AppInfo", message)
//    }
//
//    // 记录调试信息
//    fun debug(message: String) {
//        Logger.debug(message as Any)
//        Log.d("AppDebug", message)
//    }
}