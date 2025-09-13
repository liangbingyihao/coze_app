package sdk.chat.demo.robot.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object LogHelper {
    private const val LOG_CLEAR_INTERVAL = 120_000L // 2分钟（毫秒）
    var logStr = ""
        private set // 外部可读不可直接修改
    private var lastLogTime = 0L

    /**
     * 添加带时间戳的日志
     * @param newLog 日志内容（自动添加时间前缀）
     */
    fun appendLog(newLog: String) {
        val currentTime = System.currentTimeMillis()

        // 超过间隔时间则清空旧日志
        if (currentTime - lastLogTime > LOG_CLEAR_INTERVAL) {
            logStr = ""
        }

        // 格式化时间戳 + 日志内容
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date(currentTime))
        logStr += "\n[$timestamp] $newLog"

        lastLogTime = currentTime
    }

    /**
     * 获取当前所有日志（可选）
     */
    fun getFormattedLogs(): String {
        return logStr.trimStart('\n') // 去除首行空行
    }

    /**
     * 清空日志（可选）
     */
    fun clearLogs() {
        logStr = ""
        lastLogTime = 0L
    }

    fun reportExportEvent(event: String, log: String,  throwable:Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()


        // 记录基本信息
        crashlytics.log(log)


        // 设置自定义键
        crashlytics.setCustomKey("event", event)


        // 如果有错误
        if (throwable!=null) {
            crashlytics.recordException(throwable)
        }

    }
}