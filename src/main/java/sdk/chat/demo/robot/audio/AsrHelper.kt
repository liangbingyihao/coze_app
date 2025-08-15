package sdk.chat.demo.robot.audio

import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import org.json.JSONException
import org.json.JSONObject
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.extensions.SpeechStreamRecorder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object AsrHelper {
    private const val TAG = "AsrHelper"

    // Engine
    private var mSpeechEngine: SpeechEngine? = null
    private var mEngineStarted = false
    private var mStreamRecorder: SpeechStreamRecorder? = null

    private var mSpeechListener: SpeechEngine.SpeechListener =
        object : SpeechEngine.SpeechListener {
            override fun onSpeechMessage(
                type: Int,
                data: ByteArray?,
                len: Int
            ) {

                val stdData = kotlin.text.String(data!!)
                when (type) {
                    SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                        // Callback: 引擎启动成功回调
                        Log.i(TAG, "Callback: 引擎启动成功: data: " + stdData)
                        mEngineStarted = true
//                resultTextView.setText(resultTextView.text.toString()+"\nCallback: 引擎启动成功: data: " + stdData)
//                speechStart()
                    }

                    SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                        // Callback: 引擎关闭回调
                        Log.i(TAG, "Callback: 引擎关闭: data: " + stdData)
//                resultTextView.setText(resultTextView.text.toString()+"\nCallback: 引擎关闭: data:  " + stdData)
                        mEngineStarted = false
                        ChatSDK.events().source()
                            .accept(NetworkEvent.messageInputAsr(null,null, false))
//                speechStop()
                    }

                    SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                        // Callback: 错误信息回调
                        Log.e(TAG, "Callback: 错误信息: " + stdData)
                        speechError(stdData)
                    }

                    SpeechEngineDefines.MESSAGE_TYPE_CONNECTION_CONNECTED -> Log.i(
                        TAG,
                        "Callback: 建连成功: data: " + stdData
                    )

                    SpeechEngineDefines.MESSAGE_TYPE_PARTIAL_RESULT -> {
                        // Callback: ASR 当前请求的部分结果回调
//                        Log.d(TAG, "Callback: ASR 当前请求的部分结果")
                        speechAsrResult(stdData, false)
                    }

                    SpeechEngineDefines.MESSAGE_TYPE_FINAL_RESULT -> {
                        // Callback: ASR 当前请求最终结果回调
                        Log.i(TAG, "Callback: ASR 当前请求最终结果")
                        speechAsrResult(stdData, true)
                    }

                    SpeechEngineDefines.MESSAGE_TYPE_VOLUME_LEVEL ->                 // Callback: 录音音量回调
                        Log.d(TAG, "Callback: 录音音量")

                    else -> {}
                }
            }

        }

    private var lastAsrResult: String = ""
    fun speechAsrResult(data: String, isFinal: Boolean) {
        try {
            // 从回调的 json 数据中解析 ASR 结果
            val reader = JSONObject(data)
            if (!reader.has("result")) {
                return
            }
            var text = reader.getJSONArray("result").getJSONObject(0).getString("text")
            if (text.isEmpty()) {
                return
            }

//            Log.i(TAG, text)
            if (text != lastAsrResult) {
                ChatSDK.events().source()
                    .accept(NetworkEvent.messageInputAsr(lastAsrResult, text, true))
            }
            lastAsrResult = text
//            if (isFinal) {
//                text += "\nreqid: " + reader.getString("reqid")
//            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun speechError(data: String) {
        try {
            // 从回调的 json 数据中解析错误码和错误详细信息
            val reader = JSONObject(data)
            if (!reader.has("err_code") || !reader.has("err_msg")) {
                return
            }
            //notify data
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun initAsrEngine() {
        mStreamRecorder = SpeechStreamRecorder()

        if (mSpeechEngine == null) {
            Log.i(TAG, "创建引擎.")
            mSpeechEngine = SpeechEngineGenerator.getInstance()
            mSpeechEngine!!.createEngine()
            mSpeechEngine!!.setContext(MainApp.getContext())
        }
        Log.d(TAG, "SDK 版本号: " + mSpeechEngine!!.getVersion())

        Log.i(TAG, "配置初始化参数.")
        configInitParams()

        Log.i(TAG, "引擎初始化.")
        val ret = mSpeechEngine!!.initEngine()
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            val errMessage = "初始化失败，返回值: $ret"
            Log.e(TAG, errMessage)
            return
        }
        Log.i(TAG, "设置消息监听")
        mSpeechEngine!!.setListener(mSpeechListener)
        mStreamRecorder?.SetSpeechEngine("ASR", mSpeechEngine)
    }

    private fun configInitParams() {
        //【必需配置】Engine Name
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.ASR_ENGINE
        )

//        //【可选配置】Debug & Log
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING,
//            mDebugPath
//        )
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING,
            SpeechEngineDefines.LOG_LEVEL_DEBUG
        )

        //【可选配置】User ID（用以辅助定位线上用户问题）
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_UID_STRING,
            ChatSDK.currentUser().entityID
        )

        //【必需配置】配置音频来源
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_RECORDER_TYPE_STRING,
            SpeechEngineDefines.RECORDER_TYPE_RECORDER
        )

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

    private var recordIsRunning = false
    var executor: ExecutorService? = Executors.newFixedThreadPool(1)
    private var recordRunnable: Runnable? = null

    fun startAsr() {
        recordIsRunning = false
        recordRunnable = Runnable {
            recordIsRunning = true
            Log.i(TAG, "配置启动参数.")
            //【可选配置】该按钮为长按模式，预期是按下开始录音，抬手结束录音，需要关闭云端自动判停功能。
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_AUTO_STOP_BOOL,
                false
            )

            //【可选配置】是否开启顺滑(DDC)
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_ENABLE_DDC_BOOL,
                true
            )

            //【可选配置】是否开启标点
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_SHOW_NLU_PUNC_BOOL,
                true
            )
            //【可选配置】是否隐藏句尾标点
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_DISABLE_END_PUNC_BOOL,
                false
            )


            //【可选配置】控制识别结果返回的形式，全量返回或增量返回，默认为全量
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_ASR_RESULT_TYPE_STRING,
                SpeechEngineDefines.ASR_RESULT_TYPE_SINGLE
            )


            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(TAG, "关闭引擎（同步）")
            Log.i(TAG, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(TAG, "send directive syncstop failed, " + ret)
            } else {
                Log.i(TAG, "启动引擎")
                Log.i(TAG, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret == SpeechEngineDefines.ERR_REC_CHECK_ENVIRONMENT_FAILED) {
                    Log.e(TAG, "send directive start failed, ERR_REC_CHECK_ENVIRONMENT_FAILED")
//                    mEngineStatusTv.setText(R.string.check_rec_permission)
//                    requestPermission(com.bytedance.speech.speechdemo.AsrActivity.ASR_PERMISSIONS)
                } else if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    Log.e(TAG, "send directive start failed, " + ret)
                }
            }
        }
        executor?.execute(recordRunnable)
    }


    fun stopAsr() {
        if (recordIsRunning) {
            recordIsRunning = false
            Log.i(TAG, "AsrTouch: Finish")
            // Directive：结束用户音频输入。
            Log.i(TAG, "Directive: DIRECTIVE_FINISH_TALKING")
            mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_FINISH_TALKING, "")
            mStreamRecorder!!.Stop()
        } else if (recordRunnable != null) {
            Log.i(TAG, "AsrTouch: Cancel")
//            recordHandler!!.removeCallbacks(recordRunnable)
        }
    }
}