package sdk.chat.demo.robot.extensions

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit

object LanguageUtils {
    fun isChineseChar(c: Char): Boolean {
        return c in '\u4E00'..'\u9FA5' || c in '\u3400'..'\u4DBF'
    }

    fun getTextLanguage(text: String): Locale {
        return if (text.any { isChineseChar(it) }) Locale.CHINESE else Locale.US
    }

    private const val PREFS_LANGUAGE_KEY = "app_language"


    fun initAppLanguage(context: Context) {
        val savedLang = getSavedLanguage(context)
        val languageToSet = if (savedLang.isNotEmpty()) {
            savedLang
        } else {
            // 如果没有保存过语言，使用系统语言
            getSystemLanguage()
        }
        setAppLanguage(languageToSet)
    }

    // 切换语言并保存
    fun switchLanguage(context: Context, languageCode: String) {
        setAppLanguage(languageCode)
        saveLanguage(context, languageCode)
    }

    private fun setAppLanguage(languageCode: String) {
        val locales = if (languageCode.isNotEmpty()) {
            LocaleListCompat.forLanguageTags(languageCode)
        } else {
            // 空字符串表示使用系统默认语言
            LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun saveLanguage(context: Context, code: String) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit() {
                putString(PREFS_LANGUAGE_KEY, code)
            }
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString(PREFS_LANGUAGE_KEY, "") ?: ""
    }

    // 获取系统当前语言
    fun getSystemLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault()[0].toLanguageTag()
        } else {
            Locale.getDefault().toLanguageTag()
        }
    }
}