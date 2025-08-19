package sdk.chat.demo.robot.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleObserver
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import org.json.JSONException
import org.json.JSONObject
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.SpeechStreamRecorder
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.SpeechToTextHelper
import sdk.chat.demo.robot.push.UpdateTokenWorker
import sdk.chat.ui.utils.ToastHelper

data class SpinnerItem(val label: String, val value: String) {
    override fun toString(): String = label
}

class SpeechToTextActivity : AppCompatActivity(), View.OnClickListener, SpeechEngine.SpeechListener,
    LifecycleObserver {
    private lateinit var speechToTextHelper: SpeechToTextHelper
    private lateinit var resultTextView: TextView
    private lateinit var tvTaskIndex: EditText
    private lateinit var ttsVoiceTypeSpinner: Spinner

    // Engine
    private var mSpeechEngine: SpeechEngine? = null
    private var mEngineStarted = false

    // StreamRecorder
    private var mStreamRecorder: SpeechStreamRecorder? = null

    // Statistics
    private var mFinishTalkingTimestamp: Long = -1
//    private lateinit var startButton: Button
//    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        resultTextView = findViewById(R.id.resultTextView)
        tvTaskIndex = findViewById(R.id.taskIndex)
        findViewById<View>(R.id.updateToken).setOnClickListener(this)
        findViewById<View>(R.id.startButton).setOnClickListener(this)
        findViewById<View>(R.id.stopButton).setOnClickListener(this)
        findViewById<View>(R.id.clearCache).setOnClickListener(this)
        findViewById<View>(R.id.setTask).setOnClickListener(this)
        findViewById<View>(R.id.setPrompt).setOnClickListener(this)
//        findViewById<View>(R.id.startdbasr).setOnClickListener(this)
        findViewById<View>(R.id.initdbasr).setOnClickListener(this)
        findViewById<View>(R.id.cleardbasr).setOnClickListener(this)


        var mRecordBtn = findViewById<View>(R.id.startdbasr)

        mRecordBtn.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (event!!.getAction() == MotionEvent.ACTION_DOWN) {
                Log.i(tagAsr, "Record: Action down")
                recordBtnTouchDown()
                return@setOnTouchListener true
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.i(tagAsr, "Record: Action up")
                recordBtnTouchUp()
                return@setOnTouchListener true
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                Log.i(tagAsr, "Record: Action cancel")
                recordBtnTouchUp()
                return@setOnTouchListener true
            }
            false
        }

        // 检查并请求录音权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }

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

    }

    fun setVoiceTypeSpinner() {
        ttsVoiceTypeSpinner = findViewById(R.id.ttsVoiceTypeSpinner)
        val languages = listOf(
            SpinnerItem("通用-灿灿", "BV700_streaming"),
            SpinnerItem("通用-女声", "BV001_streaming"),
            SpinnerItem("通用-男声", "BV002_streaming"),
            SpinnerItem("有声阅读-擎苍", "BV701_streaming"),
            SpinnerItem("有声阅读-通用赘婿", "BV119_streaming"),
            SpinnerItem("有声阅读-儒雅青年", "BV102_streaming"),
            SpinnerItem("有声阅读-甜宠少御", "BV113_streaming"),
            SpinnerItem("有声阅读-甜宠少御", "BV115_streaming"),
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ttsVoiceTypeSpinner.adapter = adapter

        var voiceType =
            getSharedPreferences("app_prefs", MODE_PRIVATE).getString(
                "db_voice_type",
                "BV026_streaming"
            )

        for (i in 0..<adapter.getCount()) {
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
                TTSHelper.voiceType = selected.value
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要录音权限才能使用语音识别", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechToTextHelper.destroy()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.updateToken -> {
                UpdateTokenWorker.forceUpdateToken(this@SpeechToTextActivity)
            }

            R.id.startButton -> {
                speechToTextHelper.startListening()
            }

            R.id.stopButton -> {
                speechToTextHelper.stopListening()
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
            }

            R.id.setTask -> {
                var taskIndex: Int = tvTaskIndex.text.toString().toInt()
                DailyTaskHandler.testTaskDetail(taskIndex)
            }

            R.id.initdbasr -> {
                if (mEngineStarted) {
                    return
                }
                initEngine()
            }

            R.id.cleardbasr -> {
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_STOP_ENGINE, "")
            }
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val tagAsr = "doubaoasr"
    }

    private fun initEngine() {
        mStreamRecorder = SpeechStreamRecorder()

        if (mSpeechEngine == null) {
            Log.i(tagAsr, "创建引擎.")
            mSpeechEngine = SpeechEngineGenerator.getInstance()
            mSpeechEngine!!.createEngine()
            mSpeechEngine!!.setContext(applicationContext)
        }
        Log.d(tagAsr, "SDK 版本号: " + mSpeechEngine!!.getVersion())

        Log.i(tagAsr, "配置初始化参数.")
        configInitParams()

        Log.i(tagAsr, "引擎初始化.")
        val ret = mSpeechEngine!!.initEngine()
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            val errMessage = "初始化失败，返回值: " + ret
            Log.e(tagAsr, errMessage)
            speechEngineInitFailed(errMessage)
            return
        }
        Log.i(tagAsr, "设置消息监听")
        mSpeechEngine!!.setListener(this)
        speechEnginInitucceeded()
    }

    fun speechEngineInitFailed(tipText: String?) {
        Log.e(tagAsr, "引擎初始化失败: " + tipText)
        this.runOnUiThread({
            resultTextView.setText("引擎初始化失败: " + tipText)
        })
    }

    fun speechEnginInitucceeded() {
        Log.i(tagAsr, "引擎初始化成功!")
        mStreamRecorder?.SetSpeechEngine("ASR", mSpeechEngine)
        ToastHelper.show(this@SpeechToTextActivity, "引擎初始化成功!")
//        this.runOnUiThread({
//
//        })
    }

    private fun configInitParams() {
        //【必需配置】Engine Name
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.ASR_ENGINE
        )

        //【可选配置】Debug & Log
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING,
            mDebugPath
        )
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING,
            SpeechEngineDefines.LOG_LEVEL_DEBUG
        )

        //【可选配置】User ID（用以辅助定位线上用户问题）
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_UID_STRING,
            "gw12345678"
        )

        //【必需配置】配置音频来源
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_RECORDER_TYPE_STRING,
            SpeechEngineDefines.RECORDER_TYPE_RECORDER
        )


        //【可选配置】录音文件保存路径，如配置，SDK会将录音保存到该路径下，文件格式为 .wav
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_REC_PATH_STRING,
            mDebugPath
        )

//        //【可选配置】音频采样率，默认16000
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_SAMPLE_RATE_INT,
//            mSettings.getInt(R.string.config_sample_rate)
//        )
//        //【可选配置】音频通道数，默认1，可选1或2
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_CHANNEL_NUM_INT,
//            mSettings.getInt(R.string.config_channel)
//        )
//        //【可选配置】上传给服务的音频通道数，默认1，可选1或2，一般与PARAMS_KEY_CHANNEL_NUM_INT保持一致即可
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_UP_CHANNEL_NUM_INT,
//            mSettings.getInt(R.string.config_channel)
//        )

//        // 当音频来源为 RECORDER_TYPE_STREAM 时，如输入音频采样率不等于 16K，需添加如下配置
//        if (mSettings.getOptionsValue(R.string.config_recorder_type, this)
//                .equals(SpeechEngineDefines.RECORDER_TYPE_STREAM)
//        ) {
//            if (mStreamRecorder.GetStreamSampleRate() !== 16000 || mStreamRecorder.GetStreamChannel() !== 1) {
//                // 当音频来源为 RECORDER_TYPE_STREAM 时【必需配置】，否则【无需配置】
//                // 启用 SDK 内部的重采样
//                mSpeechEngine!!.setOptionBoolean(
//                    SpeechEngineDefines.PARAMS_KEY_ENABLE_RESAMPLER_BOOL,
//                    true
//                )
//                // 将重采样所需的输入采样率设置为 APP 层输入的音频的实际采样率
//                mSpeechEngine!!.setOptionInt(
//                    SpeechEngineDefines.PARAMS_KEY_CUSTOM_SAMPLE_RATE_INT,
//                    mStreamRecorder.GetStreamSampleRate()
//                )
//                mSpeechEngine!!.setOptionInt(
//                    SpeechEngineDefines.PARAMS_KEY_CUSTOM_CHANNEL_INT,
//                    mStreamRecorder.GetStreamChannel()
//                )
//            }
//        }

//        var address: String = mSettings.getString(R.string.config_address)
//        if (address.isEmpty()) {
//            address = SensitiveDefines.DEFAULT_ADDRESS
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current address: " + address)
        //【必需配置】识别服务域名
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_ADDRESS_STRING,
            "wss://openspeech.bytedance.com"
        )

//        var uri: String = mSettings.getString(R.string.config_uri)
//        if (uri.isEmpty()) {
//            uri = SensitiveDefines.ASR_DEFAULT_URI
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current uri: " + uri)
        //【必需配置】识别服务Uri
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_URI_STRING,
            "/api/v2/asr"
        )

//        var appid: String = mSettings.getString(R.string.config_app_id)
//        if (appid.isEmpty()) {
//            appid = SensitiveDefines.APPID
//        }
        //【必需配置】鉴权相关：Appid
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, "2617262954")

//        val token: String? = mSettings.getString(R.string.config_token)
        //【必需配置】鉴权相关：Token
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING,
            "Bearer;Yn2tI0wSDWOgEJhP8PPLcXtGInB11N4M"
        )

//        var cluster: String = mSettings.getString(R.string.config_cluster)
//        if (cluster.isEmpty()) {
//            cluster = SensitiveDefines.ASR_DEFAULT_CLUSTER
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current cluster: " + cluster)
        //【必需配置】识别服务所用集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_CLUSTER_STRING,
            "volcengine_input_common"
        )

        //【可选配置】在线请求的建连与接收超时，一般不需配置使用默认值即可
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_ASR_CONN_TIMEOUT_INT, 3000)
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_ASR_RECV_TIMEOUT_INT, 5000)

        //【可选配置】在线请求断连后，重连次数，默认值为0，如果需要开启需要设置大于0的次数
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_ASR_MAX_RETRY_TIMES_INT,
            2
        )
    }

    // Debug path
    private var mDebugPath: String? = null

    fun getDebugPath(): String {
        mDebugPath?.let { return it }
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            Log.d(tagAsr, "External storage can be read and write.")
        } else {
            Log.e(tagAsr, "External storage can't write.")
            return ""
        }
        val debugDir = getExternalFilesDir(null)
        if (debugDir == null) {
            return ""
        }
        if (!debugDir.exists()) {
            if (debugDir.mkdirs()) {
                Log.d(tagAsr, "Create debug path successfully.")
            } else {
                Log.e(tagAsr, "Failed to create debug path.")
                return ""
            }
        }
        mDebugPath = debugDir.absolutePath
        return mDebugPath.toString()
    }

    override fun onSpeechMessage(type: Int, data: ByteArray?, len: Int) {
        val stdData = kotlin.text.String(data!!)
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                // Callback: 引擎启动成功回调
                Log.i(tagAsr, "Callback: 引擎启动成功: data: " + stdData)
                mEngineStarted = true
//                resultTextView.setText(resultTextView.text.toString()+"\nCallback: 引擎启动成功: data: " + stdData)
//                speechStart()
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                // Callback: 引擎关闭回调
                Log.i(tagAsr, "Callback: 引擎关闭: data: " + stdData)
//                resultTextView.setText(resultTextView.text.toString()+"\nCallback: 引擎关闭: data:  " + stdData)
                mEngineStarted = false
//                speechStop()
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                // Callback: 错误信息回调
                Log.e(tagAsr, "Callback: 错误信息: " + stdData)
                speechError(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_CONNECTION_CONNECTED -> Log.i(
                tagAsr,
                "Callback: 建连成功: data: " + stdData
            )

            SpeechEngineDefines.MESSAGE_TYPE_PARTIAL_RESULT -> {
                // Callback: ASR 当前请求的部分结果回调
                Log.d(tagAsr, "Callback: ASR 当前请求的部分结果")
                speechAsrResult(stdData, false)
            }

            SpeechEngineDefines.MESSAGE_TYPE_FINAL_RESULT -> {
                // Callback: ASR 当前请求最终结果回调
                Log.i(tagAsr, "Callback: ASR 当前请求最终结果")
                speechAsrResult(stdData, true)
            }

            SpeechEngineDefines.MESSAGE_TYPE_VOLUME_LEVEL ->                 // Callback: 录音音量回调
                Log.d(tagAsr, "Callback: 录音音量")

            else -> {}
        }
    }

    fun speechError(data: String) {
        this.runOnUiThread({
            try {
                // 从回调的 json 数据中解析错误码和错误详细信息
                val reader = JSONObject(data)
                if (!reader.has("err_code") || !reader.has("err_msg")) {
                    return@runOnUiThread
                }
                resultTextView.setText(resultTextView.text.toString() + "\n" + data)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
    }

    fun speechAsrResult(data: String, isFinal: Boolean) {
        // 计算由录音结束到 ASR 最终结果之间的延迟
        var delay: Long = 0
        if (isFinal && mFinishTalkingTimestamp > 0) {
            delay = System.currentTimeMillis() - mFinishTalkingTimestamp
            mFinishTalkingTimestamp = 0
        }
        val response_delay = delay
        this.runOnUiThread({
            try {
                // 从回调的 json 数据中解析 ASR 结果
                val reader = JSONObject(data)
                if (!reader.has("result")) {
                    return@runOnUiThread
                }
                var text = reader.getJSONArray("result").getJSONObject(0).getString("text")
                if (text.isEmpty()) {
                    return@runOnUiThread
                }
                text = "result: " + text
                if (isFinal) {
                    text += "\nreqid: " + reader.getString("reqid")
                    text += "\nresponse_delay: " + response_delay
                }
                resultTextView.setText(resultTextView.text.toString() + text)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
    }

    // Record
    private var recordHandler: Handler? = null
    private lateinit var recordRunnable: Runnable
    private var recordIsRunning = false
    private fun recordBtnTouchDown() {
        recordIsRunning = false
        recordHandler = Handler()
        recordRunnable = Runnable {
            recordIsRunning = true
            Log.i(tagAsr, "配置启动参数.")
            configStartAsrParams()
            //【可选配置】该按钮为长按模式，预期是按下开始录音，抬手结束录音，需要关闭云端自动判停功能。
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_AUTO_STOP_BOOL,
                false
            )

            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(tagAsr, "关闭引擎（同步）")
            Log.i(tagAsr, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(tagAsr, "send directive syncstop failed, " + ret)
            } else {
                Log.i(tagAsr, "启动引擎")
                Log.i(tagAsr, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret == SpeechEngineDefines.ERR_REC_CHECK_ENVIRONMENT_FAILED) {
                    Log.e(tagAsr, "send directive start failed, ERR_REC_CHECK_ENVIRONMENT_FAILED")
//                    mEngineStatusTv.setText(R.string.check_rec_permission)
//                    requestPermission(com.bytedance.speech.speechdemo.AsrActivity.ASR_PERMISSIONS)
                } else if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    Log.e(tagAsr, "send directive start failed, " + ret)
                }
            }
            resultTextView.setText("")
        }
        recordHandler?.postDelayed(recordRunnable, 500)
    }

    private fun recordBtnTouchUp() {
        if (recordIsRunning) {
            recordIsRunning = false
            Log.i(tagAsr, "AsrTouch: Finish")
            mFinishTalkingTimestamp = System.currentTimeMillis()
            // Directive：结束用户音频输入。
            Log.i(tagAsr, "Directive: DIRECTIVE_FINISH_TALKING")
            mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_FINISH_TALKING, "")
            mStreamRecorder!!.Stop()
        } else if (recordRunnable != null) {
            Log.i(tagAsr, "AsrTouch: Cancel")
            recordHandler!!.removeCallbacks(recordRunnable)
        }
    }

    private fun configStartAsrParams() {
//        //【可选配置】是否开启顺滑(DDC)
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ASR_ENABLE_DDC_BOOL,
//            mSettings.getBoolean(R.string.config_asr_enable_ddc)
//        )
//        //【可选配置】是否开启文字转数字(ITN)
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ASR_ENABLE_ITN_BOOL,
//            mSettings.getBoolean(R.string.config_asr_enable_itn)
//        )
//        //【可选配置】是否开启标点
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ASR_SHOW_NLU_PUNC_BOOL,
//            mSettings.getBoolean(R.string.config_asr_enable_nlu_punctuation)
//        )
//        //【可选配置】设置识别语种
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_ASR_LANGUAGE_STRING,
//            mSettings.getString(R.string.config_asr_language)
//        )
//        //【可选配置】是否启用云端自动判停
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ASR_AUTO_STOP_BOOL,
//            mSettings.getBoolean(R.string.config_asr_auto_stop)
//        )
//        //【可选配置】是否隐藏句尾标点
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ASR_DISABLE_END_PUNC_BOOL,
//            mSettings.getBoolean(R.string.config_asr_disable_end_punctuation)
//        )
//
//        //【可选配置】控制识别结果返回的形式，全量返回或增量返回，默认为全量
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_ASR_RESULT_TYPE_STRING,
//            mSettings.getOptionsValue(R.string.config_asr_result_type, this)
//        )
//
//        //【可选配置】设置VAD头部静音时长，用户多久没说话视为空音频，即静音检测时长
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_ASR_VAD_START_SILENCE_TIME_INT,
//            mSettings.getInt(R.string.config_asr_vad_start_silence_time)
//        )
//        //【可选配置】设置VAD尾部静音时长，用户说话后停顿多久视为说话结束，即自动判停时长
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_ASR_VAD_END_SILENCE_TIME_INT,
//            mSettings.getInt(R.string.config_asr_vad_end_silence_time)
//        )
//        //【可选配置】设置VAD模式，用于定制VAD场景，默认为空
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_ASR_VAD_MODE_STRING,
//            mSettings.getString(R.string.config_asr_vad_mode)
//        )
//        //【可选配置】用户音频输入最大时长，仅一句话识别场景生效，单位毫秒，默认为 60000ms.
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_VAD_MAX_SPEECH_DURATION_INT,
//            mSettings.getInt(R.string.config_vad_max_speech_duration)
//        )
//
//        //【可选配置】控制是否返回录音音量，在 APP 需要显示音频波形时可以启用
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_ENABLE_GET_VOLUME_BOOL,
//            mSettings.getBoolean(R.string.config_get_volume)
//        )
//
//        //【可选配置】设置纠错词表，识别结果会根据设置的纠错词纠正结果，例如："{\"古爱玲\":\"谷爱凌\"}"，当识别结果中出现"古爱玲"时会替换为"谷爱凌"
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_ASR_CORRECT_WORDS_STRING,
//            mSettings.getString(R.string.config_asr_correct_words)
//        )
//
//
//        //【可选配置】更新 ASR 热词
//        if (!mSettings.getString(R.string.config_asr_hotwords).isEmpty()) {
//            Log.d(SpeechDemoDefines.TAG, "Set hotwords.")
//            setHotWords(mSettings.getString(R.string.config_asr_hotwords))
//        }
//
//        if (mSettings.getOptionsValue(R.string.config_recorder_type, this)
//                .equals(SpeechEngineDefines.RECORDER_TYPE_STREAM)
//        ) {
//            if (!mStreamRecorder!!.Start()) {
//                requestPermission(com.bytedance.speech.speechdemo.AsrActivity.ASR_PERMISSIONS)
//            }
//        } else if (mSettings.getOptionsValue(R.string.config_recorder_type, this)
//                .equals(SpeechEngineDefines.RECORDER_TYPE_FILE)
//        ) {
//            // 使用音频文件识别时，需要设置文件的绝对路径
//            val test_file_path = mDebugPath + "/asr_rec_file.pcm"
//            Log.d(SpeechDemoDefines.TAG, "输入的音频文件路径: " + test_file_path)
//            // 使用音频文件识别时【必须配置】，否则【无需配置】
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_RECORDER_FILE_STRING,
//                test_file_path
//            )
//        }
    }

}