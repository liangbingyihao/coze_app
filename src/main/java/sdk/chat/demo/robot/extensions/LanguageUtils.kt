package sdk.chat.demo.robot.extensions

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import android.icu.text.Transliterator
import androidx.annotation.StringRes
import com.zqc.opencc.android.lib.ChineseConverter
import com.zqc.opencc.android.lib.ConversionType
import org.tinylog.Logger
import sdk.chat.demo.MainApp
import java.lang.ref.WeakReference

object LanguageUtils {
    private var contextRef: WeakReference<Context>? = null

    fun updateContext(context: Context) {
        contextRef = WeakReference(context)
    }

    fun getString(@StringRes resId: Int): String? {
        // 尝试从弱引用获取 Context
        val context = contextRef?.get()
        return context?.// 如果弱引用有效，使用它获取字符串
        getString(resId) ?: // 如果弱引用失效，回退到 Application Context
        MainApp.getContext().getString(resId)
    }

    fun isChineseChar(c: Char): Boolean {
        return c in '\u4E00'..'\u9FA5' || c in '\u3400'..'\u4DBF'
    }

    fun getTextLanguage(text: String): Locale {
        return if (text.any { isChineseChar(it) }) Locale.CHINESE else Locale.US
    }

    fun simplifiedToTraditional(context:Context,text: String): String {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val converter = Transliterator.getInstance("Simplified-Traditional")
//            return converter.transliterate(text)
//        } else {
//            return ChineseConverter.convert(text,ConversionType.S2T,context)
//        }
        return ChineseConverter.convert(text,ConversionType.S2T,context)
    }

    private const val PREFS_LANGUAGE_KEY = "app_language"
    private var currentLang: String? = null


    fun initAppLanguage(context: Context) {
        val savedLang = getSavedLanguage(context)
        val languageToSet = if (savedLang.isNotEmpty()) {
            savedLang
        } else {
            // 如果没有保存过语言，使用系统语言
            getSystemLanguage()
        }

        if(languageToSet.contains("zh",false) && (languageToSet.contains("tw",false)||languageToSet.contains("hk",false))){
            Logger.info {"initAppLanguage and switchLanguage $languageToSet to zh-Hant"}
            switchLanguage(context,"zh-Hant")
        }else{
            setAppLanguage(languageToSet)
        }
    }

    fun getAppLanguage(context: Context, refresh: Boolean): String? {
        if (!refresh &&currentLang!=null&& currentLang!!.isNotEmpty()) {
            return currentLang
        }
        val savedLang = getSavedLanguage(context)
        currentLang = if (savedLang.isNotEmpty()) {
            savedLang
        } else {
            // 如果没有保存过语言，使用系统语言
            getSystemLanguage()
        }
        return currentLang
    }

    // 切换语言并保存
    fun switchLanguage(context: Context, languageCode: String) {
        setAppLanguage(languageCode)
        saveLanguage(context, languageCode)
        getAppLanguage(context, true)
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
        var lang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault()[0].toLanguageTag()
        } else {
            Locale.getDefault().toLanguageTag()
        }
        return lang
    }
}