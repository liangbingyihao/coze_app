package sdk.chat.demo.robot.extensions

import java.util.Locale

object LanguageUtils {
    fun isChineseChar(c: Char): Boolean {
        return c in '\u4E00'..'\u9FA5' || c in '\u3400'..'\u4DBF'
    }

    fun getTextLanguage(text: String): Locale {
        return if (text.any { isChineseChar(it) }) Locale.CHINESE else Locale.US
    }
}