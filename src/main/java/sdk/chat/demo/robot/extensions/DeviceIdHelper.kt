package sdk.chat.demo.robot.extensions

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID
import androidx.core.content.edit

object DeviceIdHelper {
    fun getDeviceId(context: Context): String {
        val sharedPref = context.getSharedPreferences("device_id", Context.MODE_PRIVATE)

        // 1. 优先尝试读取之前保存的ID
        sharedPref.getString("fallback_id", null)?.let {
            return it
        }

        // 2. 如果没有保存过，则生成新ID
        return getOptimizedDeviceId(context)
    }

    fun getOptimizedDeviceId(context: Context): String {
        // 1. 尝试获取 ANDROID_ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )?.takeUnless { it == "9774d56d682e549c" || it.isNullOrEmpty() }

        // 2. 获取设备硬件特征（修正后的版本）
        val hardwareId = """
        ${Build.BRAND}
        ${Build.MODEL}
        ${Build.BOARD}
        ${Build.HARDWARE}
        ${Build.FINGERPRINT}
        ${Build.getRadioVersion()}
        ${context.resources.displayMetrics}_${System.currentTimeMillis()}
    """.trimIndent().hashCode().toString()

        // 3. 获取存储路径特征
        val storagePath = context.filesDir.absolutePath

        // 4. 组合生成最终ID
        return ((androidId ?: (hardwareId + storagePath))).let {
            UUID.nameUUIDFromBytes(it.toByteArray()).toString()
        }.also { id ->
            // 保存到 SharedPreferences 作为后备
            context.getSharedPreferences("device_id", Context.MODE_PRIVATE)
                .edit() {
                    putString("fallback_id", id)
                }
        }
    }
}