package sdk.chat.demo.robot.handlers

import android.content.SharedPreferences
import android.content.Context
import androidx.core.content.edit

data class CounterItem(
    val key: String,
    var count: Int,
    var lastAccessTime: Long
)

object FrequencyCounter {
    private const val PREFS_NAME = "FrequencyCounter"
    private const val DELIMITER = "||"
    private const val ITEM_DELIMITER = ";;"

    // 增加计数
    fun increment(context: Context?, key: String) {
        if(context==null){
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val items = loadAll(prefs)

        val existing = items.find { it.key == key }
        if (existing != null) {
            existing.count++
            existing.lastAccessTime = System.currentTimeMillis()
        } else {
            items.add(CounterItem(key, 1, System.currentTimeMillis()))
        }

        saveAll(prefs, items)
    }

    // 获取排序后的列表（高频在前）
    fun getSortedItems(context: Context): List<CounterItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return loadAll(prefs).sortedWith(compareByDescending<CounterItem> { it.count }
            .thenByDescending { it.lastAccessTime })
    }

    // 清除所有记录
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { clear() }
    }

    // 私有方法：加载所有记录
    private fun loadAll(prefs: SharedPreferences): MutableList<CounterItem> {
        val allEntries = prefs.all
        return allEntries.mapNotNull { (key, value) ->
            val parts = (value as? String)?.split(DELIMITER)
            if (parts != null && parts.size == 2) {
                CounterItem(
                    key = key,
                    count = parts[0].toIntOrNull() ?: 0,
                    lastAccessTime = parts[1].toLongOrNull() ?: 0L
                )
            } else {
                null
            }
        }.toMutableList()
    }

    // 私有方法：保存所有记录
    private fun saveAll(prefs: SharedPreferences, items: List<CounterItem>) {
        prefs.edit() {
            clear() // 先清空再重新写入

            items.forEach { item ->
                putString(
                    item.key,
                    "${item.count}$DELIMITER${item.lastAccessTime}"
                )
            }

        }
    }
}