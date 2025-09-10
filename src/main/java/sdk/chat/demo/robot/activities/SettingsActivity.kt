package sdk.chat.demo.robot.activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.activities.SettingLangsActivity
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.ExportInfo
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.ui.activities.BaseActivity
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.RX

class SettingsActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvLang: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.export).setOnClickListener(this)
        findViewById<View>(R.id.debug).setOnClickListener(this)
        findViewById<View>(R.id.config_lang).setOnClickListener(this)
        tvLang = findViewById<TextView>(R.id.lang_value)
        getSettings()
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

            R.id.export -> {
                getExportInfo()
            }

            R.id.config_lang -> {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        SettingLangsActivity::class.java
                    )
                )
            }

            R.id.debug -> {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        SpeechToTextActivity::class.java
                    )
                )
            }
        }
    }

    fun getSettings() {
        try {
            var versionName = packageManager
                .getPackageInfo(packageName, 0)
                .versionName
                ?: "Unknown"
            findViewById< TextView>(R.id.version).text = versionName
        } catch (e: Exception) {
            "Unknown"
        }

        var lang = LanguageUtils.getSavedLanguage(this)
        var rid = 0
        when (lang) {
            "zh-Hans" -> {
                rid = R.string.lan_zh
            }

            "zh-Hant" -> {
                rid = R.string.lan_zh_hant
            }

            "en-US" -> {
                rid = R.string.lan_en
            }
        }
        if (rid > 0) {
            tvLang.setText(rid)
        }else{
            tvLang.text = LanguageUtils.getSystemLanguage()
        }

    }


    fun getExportInfo() {
        dm.add(
            ImageApi.getExploreInfo()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { exportInfo ->

//                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
//                        val clip = ClipData.newPlainText("恩语", exportInfo.downLoadUrl)
//                        clipboard.setPrimaryClip(clip)
//                        ToastHelper.show(
//                            this@SettingsActivity,
//                            R.string.export_hint
//                        )
                        showExportMenus(exportInfo)
                    },
                    { error -> // onError
                        ToastHelper.show(
                            this@SettingsActivity,
                            "加载失败: $error"
                        )
                    })
        )

    }

    fun showExportMenus(exportInfo: ExportInfo) {
        MaterialAlertDialogBuilder(this@SettingsActivity)
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
                            this@SettingsActivity,
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
                            ToastHelper.show(this@SettingsActivity, "未找到浏览器应用")
                        }
                    } // 点击"浏览器下载"
                    2 -> dialog.dismiss() // 点击"取消"
                }
            }
            .setCancelable(true) // 点击外部可关闭
            .show()
    }
}