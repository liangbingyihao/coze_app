package sdk.chat.demo.robot.api

import android.content.Context
import com.bumptech.glide.util.LruCache
import java.io.File

object  JsonCacheManager {
    // 内存缓存 (LRU)
    private val memoryCache = LruCache<String, String>(1024 * 1024) // 1MB

    // 磁盘缓存
    fun save(context: Context, key: String, json: String) {
        memoryCache.put(key, json)
        saveJsonToFile(context, "$key.json", json)
    }

    fun get(context: Context, key: String): String? {
        return memoryCache.get(key) ?: getJsonFromFile(context, "$key.json")
    }

    fun saveJsonToFile(context: Context, fileName: String, json: String) {
        try {
            File(context.filesDir, fileName).writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getJsonFromFile(context: Context, fileName: String): String? {
        return try {
            File(context.filesDir, fileName).readText()
        } catch (e: Exception) {
            null
        }
    }
}