package sdk.chat.demo.robot.activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.ArticleListActivity
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.api.model.ExportInfo
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.RX


class SettingLangsActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_langs)
        findViewById<View>(R.id.home).setOnClickListener(this)
        initSetting()

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.postDelayed({
            radioGroup.setOnCheckedChangeListener { group, checkedId ->
//                val radioButton = findViewById<RadioButton>(checkedId)
//                ToastHelper.show(this@SettingLangsActivity, radioButton.text.toString())
                when (checkedId) {
                    R.id.radioZH -> {
                        LanguageUtils.switchLanguage(this, "zh-Hans")
                    }
                    R.id.radioHant -> {
                        LanguageUtils.switchLanguage(this, "zh-Hant")
                    }

                    R.id.radioEN -> {
                        LanguageUtils.switchLanguage(this, "en-US")
                    }

                }
                JsonCacheManager.save(this, "gwTaskProcess", "")
                JsonCacheManager.save(this, "gwDaily", "")
                TTSHelper.resetVoiceType()
                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                threadHandler.clearThreadCache()
                dm.add(
                    ImageApi.getServerConfigs()
                        .subscribeOn(Schedulers.io())
                        .observeOn(RX.main())
                        .doFinally { finish() }
                        .subscribe()
                )
            }
        }, 10)
    }

    fun initSetting() {
        var lang = LanguageUtils.getSavedLanguage(this@SettingLangsActivity)
        var rid = 0
        when (lang) {
            "zh-Hans" -> {
                rid = R.id.radioZH
            }

            "zh-Hant" -> {
                rid = R.id.radioHant
            }

            "en-US" -> {
                rid = R.id.radioEN
            }
        }
        if (rid == 0) {
            lang = LanguageUtils.getSystemLanguage()
            if (lang.contains("en", ignoreCase = true)) {
                rid = R.id.radioEN
            } else if (lang.contains("Hant", ignoreCase = true)) {
                rid = R.id.radioHant
            } else if (lang.contains("zh", ignoreCase = true)) {
                rid = R.id.radioZH
            }
        }
        if (rid > 0) {
            val radioButton = findViewById<RadioButton?>(rid)
            radioButton.setChecked(true)
        }
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

        }
    }

//    fun getSettings() {
//        try {
//            packageManager
//                .getPackageInfo(packageName, 0)
//                .versionName
//                ?: "Unknown"
//        } catch (e: Exception) {
//            "Unknown"
//        }
//
//        AppCompatDelegate.setApplicationLocales(
//            LocaleListCompat.forLanguageTags("zh-CN")
//        )
//    }
//
//
//    fun getExportInfo() {
//        dm.add(
//            ImageApi.getExploreInfo()
//                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
//                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
//                .subscribe(
//                    { exportInfo ->
//
////                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
////                        val clip = ClipData.newPlainText("恩语", exportInfo.downLoadUrl)
////                        clipboard.setPrimaryClip(clip)
////                        ToastHelper.show(
////                            this@SettingsActivity,
////                            R.string.export_hint
////                        )
//                        showExportMenus(exportInfo)
//                    },
//                    { error -> // onError
//                        ToastHelper.show(
//                            this@SettingLangsActivity,
//                            error.message
//                        )
//                    })
//        )
//
//    }

    fun showExportMenus(exportInfo: ExportInfo) {
        MaterialAlertDialogBuilder(this@SettingLangsActivity)
            .setTitle(getString(R.string.setting_export)) // 可选标题
            .setItems(
                arrayOf(
                    getString(R.string.export_copy),
                    getString(R.string.export_open),
                    getString(R.string.cancel)
                )
            ) { dialog, which ->
                when (which) {
                    0 -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("恩语", exportInfo.downLoadUrl)
                        clipboard.setPrimaryClip(clip)
                        ToastHelper.show(
                            this@SettingLangsActivity,
                            R.string.export_hint
                        )
                    }    // 点击"拷贝链接"
                    1 -> {
                        try {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(exportInfo.downLoadUrl))
                            // 确保只有浏览器能处理此Intent（排除其他可能的应用）
                            intent.addCategory(Intent.CATEGORY_BROWSABLE)
                            // 禁止弹出应用选择对话框（直接使用默认浏览器）
                            intent.setPackage(null)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            ToastHelper.show(this@SettingLangsActivity, "未找到浏览器应用")
                        }
                    } // 点击"浏览器下载"
                    2 -> dialog.dismiss() // 点击"取消"
                }
            }
            .setCancelable(true) // 点击外部可关闭
            .show()
    }
}