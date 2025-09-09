package sdk.chat.demo.robot.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.LifecycleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.audio.AsrHelper
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.extensions.LogHelper
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.SpeechToTextHelper
import sdk.chat.demo.robot.push.UpdateTokenWorker
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.DisposableMap

data class SpinnerItem(val label: String, val value: String) {
    override fun toString(): String = label
}

class SpeechToTextActivity : AppCompatActivity(), View.OnClickListener,
    LifecycleObserver {
    private lateinit var speechToTextHelper: SpeechToTextHelper
    private lateinit var resultTextView: TextView
    private lateinit var ttsParams: TextView
    private lateinit var ttsError: TextView
    private lateinit var tvTaskIndex: EditText
    private lateinit var ttsVoiceTypeSpinner: Spinner
    protected var dm: DisposableMap = DisposableMap()

//    // Engine
//    private var mSpeechEngine: SpeechEngine? = null
//    private var mEngineStarted = false
//
//    // StreamRecorder
//    private var mStreamRecorder: SpeechStreamRecorder? = null

    // Statistics
    private var mFinishTalkingTimestamp: Long = -1
//    private lateinit var startButton: Button
//    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        resultTextView = findViewById(R.id.resultTextView)
        tvTaskIndex = findViewById(R.id.taskIndex)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.updateToken).setOnClickListener(this)
        findViewById<View>(R.id.startButton).setOnClickListener(this)
        findViewById<View>(R.id.stopButton).setOnClickListener(this)
        findViewById<View>(R.id.clearCache).setOnClickListener(this)
        findViewById<View>(R.id.setTask).setOnClickListener(this)
        findViewById<View>(R.id.setPrompt).setOnClickListener(this)
        findViewById<View>(R.id.getLog).setOnClickListener(this)
//        findViewById<View>(R.id.startdbasr).setOnClickListener(this)
//        findViewById<View>(R.id.initdbasr).setOnClickListener(this)
//        findViewById<View>(R.id.cleardbasr).setOnClickListener(this)

        ttsParams = findViewById<TextView>(R.id.ttsparams)
        ttsError = findViewById<TextView>(R.id.ttserror)


//        var mRecordBtn = findViewById<View>(R.id.startdbasr)
//
//        mRecordBtn.setOnTouchListener { v: View?, event: MotionEvent? ->
//            if (event!!.getAction() == MotionEvent.ACTION_DOWN) {
//                Log.i(tagAsr, "Record: Action down")
//                recordBtnTouchDown()
//                return@setOnTouchListener true
//            } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                Log.i(tagAsr, "Record: Action up")
//                recordBtnTouchUp()
//                return@setOnTouchListener true
//            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//                Log.i(tagAsr, "Record: Action cancel")
//                recordBtnTouchUp()
//                return@setOnTouchListener true
//            }
//            false
//        }

//        // 检查并请求录音权限
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.RECORD_AUDIO),
//                REQUEST_RECORD_AUDIO_PERMISSION
//            )
//        }

        // 初始化语音识别
        speechToTextHelper = SpeechToTextHelper(this) { text, isFinal ->
            runOnUiThread {
                if (isFinal) {
                    resultTextView.text = "识别结果: $text"
                    Toast.makeText(this, "识别完成", Toast.LENGTH_SHORT).show()
                } else {
                    resultTextView.text = "识别中: $text"
                }
            }
        }

        setVoiceTypeSpinner()



        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.Error))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    val params = networkEvent!!.getData()
                    val errType = params.getOrDefault("type", "") as String?
                    val msg = params.getOrDefault("msg", "tts error...") as String?
                    if ("tts" == errType) {
                        ToastHelper.show(this, msg)
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("恩语", msg)
                        clipboard.setPrimaryClip(clip)
                        ttsError.setText(msg)
                    }else if("tts.params"==errType){
                        ttsParams.setText(msg)
                    }
                })
        )
    }

    fun setVoiceTypeSpinner() {
        ttsVoiceTypeSpinner = findViewById(R.id.ttsVoiceTypeSpinner)
        var voiceTypes = ImageApi.getGwConfigs().dbVoiceTypes
        var languages = voiceTypes.map { item->SpinnerItem(item.name, item.voiceType) }
//        val languages = listOf(
//            SpinnerItem("通用-灿灿", "BV700_streaming"),
//            SpinnerItem("通用-女声", "BV001_streaming"),
//            SpinnerItem("通用-男声", "BV002_streaming"),
//            SpinnerItem("有声阅读-擎苍", "BV701_streaming"),
//            SpinnerItem("有声阅读-通用赘婿", "BV119_streaming"),
//            SpinnerItem("有声阅读-儒雅青年", "BV102_streaming"),
//            SpinnerItem("有声阅读-甜宠少御", "BV113_streaming"),
//            SpinnerItem("有声阅读-甜宠少御", "BV115_streaming"),
//        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ttsVoiceTypeSpinner.adapter = adapter

        var voiceType =
            getSharedPreferences("app_prefs", MODE_PRIVATE).getString(
                "db_voice_type",
                "BV026_streaming"
            )

        for (i in 0..<adapter.count) {
            if (adapter.getItem(i)?.value == voiceType) {
                ttsVoiceTypeSpinner.setSelection(i)
                break
            }
        }

        ttsVoiceTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = languages[position]
                // 使用selected.value获取实际值
                Toast.makeText(
                    this@SpeechToTextActivity,
                    "Selected value: ${selected.value}",
                    Toast.LENGTH_SHORT
                ).show()
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit() {
                        putString("db_voice_type", selected.value)
                    }
                if(TTSHelper.voiceType!=selected.value){
                    TTSHelper.resetVoiceType()
                }
                TTSHelper.voiceType = selected.value
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            REQUEST_RECORD_AUDIO_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "需要录音权限才能使用语音识别", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        speechToTextHelper.destroy()
        dm.dispose();
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.home ->{
                finish()
            }

            R.id.updateToken -> {
                UpdateTokenWorker.forceUpdateToken(this@SpeechToTextActivity)
            }

            R.id.startButton -> {
//                speechToTextHelper.startListening()
                LanguageUtils.switchLanguage(this, "zh-Hans")
                recreate()
            }

            R.id.stopButton -> {
//                speechToTextHelper.stopListening()
                LanguageUtils.switchLanguage(this, "en-US")
                recreate()
            }

            R.id.setPrompt -> {
                startActivity(
                    Intent(
                        this@SpeechToTextActivity,
                        SettingPromptActivity::class.java
                    )
                )
            }

            R.id.clearCache -> {

                // 保存已经显示过引导页的状态
//                getSharedPreferences("app_prefs", MODE_PRIVATE)
//                    .edit() {
//                        putBoolean("has_shown_guide", false)
//                    }
                deleteSharedPreferences("app_prefs")

//                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
//                if(threadHandler.welcome!=null){
//                    ChatSDK.db().delete(threadHandler.welcome)
//                }
                JsonCacheManager.save(this@SpeechToTextActivity, "gwTaskProcess", "")
                JsonCacheManager.save(this@SpeechToTextActivity, "gwDaily", "")
            }

            R.id.setTask -> {
                var taskIndex: Int = tvTaskIndex.text.toString().toInt()
                DailyTaskHandler.testTaskDetail(taskIndex)
            }

            R.id.getLog ->{
                val clipboard =getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("恩语", LogHelper.logStr)
                clipboard.setPrimaryClip(clip)
            }

//            R.id.initdbasr -> {
//                if (mEngineStarted) {
//                    return
//                }
//                initEngine()
//            }
//
//            R.id.cleardbasr -> {
//                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_STOP_ENGINE, "")
//            }
        }
    }

}