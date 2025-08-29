package sdk.chat.demo.robot.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.ui.activities.BaseActivity
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.RX

class SettingsActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.export).setOnClickListener(this)
        findViewById<View>(R.id.version).setOnClickListener(this)
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
            R.id.version -> {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        SpeechToTextActivity::class.java
                    )
                )
            }
        }
    }

    fun getSettings(){
        try {
            packageManager
                .getPackageInfo(packageName, 0)
                .versionName
                ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }


    fun getExportInfo() {
        dm.add(
            ImageApi.getExploreInfo()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { exportInfo ->

                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("恩语", exportInfo.downLoadUrl)
                        clipboard.setPrimaryClip(clip)
                        ToastHelper.show(
                            this@SettingsActivity,
                            R.string.export_hint
                        )
                    },
                    { error -> // onError
                        ToastHelper.show(
                            this@SettingsActivity,
                            "加载失败: $error"
                        )
                    })
        )

    }
}