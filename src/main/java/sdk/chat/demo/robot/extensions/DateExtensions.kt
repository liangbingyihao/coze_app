package sdk.chat.demo.robot.extensions

import android.content.Context
import android.text.format.DateUtils
import sdk.chat.demo.pre.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateLocalizationUtil {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    fun getFriendlyDate(context: Context, date: Date): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        return when {
            isToday(now, target) -> context.getString(R.string.today)
            isYesterday(now, target) -> context.getString(R.string.yesterday)
            isWithinDays(now, target, 7) -> getDaysAgoText(context, now, target)
            isSameYear(now, target) -> formatDate(
                date,
                context.getString(R.string.this_year_format)
            )

            else -> formatDate(date, context.getString(R.string.default_format))
        }
    }

    fun dateStr(date: Date?): String {
        if (date == null) {
            return ""
        }
        return dayFormat.format(date)
    }

    fun toDate(dateStr: String): Date {
        try {
            return sdf.parse(dateStr)
        } catch (e: ParseException) {
        }
        return Date();
    }

    private fun isToday(cal1: Calendar, cal2: Calendar): Boolean {
        return DateUtils.isToday(cal2.timeInMillis)
    }

    private fun isYesterday(now: Calendar, target: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }
        return isSameDay(yesterday, target)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isWithinDays(now: Calendar, target: Calendar, days: Int): Boolean {
        val diff = now.timeInMillis - target.timeInMillis
        return diff > 0 && diff < days * 24 * 60 * 60 * 1000L
    }

    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun getDaysAgoText(context: Context, now: Calendar, target: Calendar): String {
        val diffDays = ((now.timeInMillis - target.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        return context.getString(R.string.days_ago, diffDays)
    }

    private fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    private fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }


    public fun formatDayAgo(dateAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1 * dateAgo) // 减去一个月
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun getDateBefore(dateStr: String?, dateAgo: Int): String {
        if (dateStr == null || dateStr.isEmpty()) {
            return formatDayAgo(dateAgo)
        }
        // 1. 解析输入日期
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate =
            inputFormat.parse(dateStr) ?: throw IllegalArgumentException("Invalid date format")

        // 2. 计算指定天数前的日期
        val calendar = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_YEAR, -dateAgo) // 减去指定天数
        }

        // 3. 格式化为字符串
        return inputFormat.format(calendar.time)
    }
}