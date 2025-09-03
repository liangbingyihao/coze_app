package sdk.chat.demo.robot.audio

import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import org.json.JSONArray
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
                            .accept(NetworkEvent.messageInputAsr(null, null, false))
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
    private var lastDefinite: Boolean = false
    private var reportIndex: Int = 1
    fun speechAsrResult(data: String, isFinal: Boolean) {
        try {

            // 从回调的 json 数据中解析 ASR 结果
//            val reader = JSONObject(data)
//            val rawResult: Any? = reader.opt("result")
//            var result: JSONObject? = null
//            when {
//                rawResult is JSONObject -> {
//                    // 处理 JSONObject
//                    result = rawResult
//                }
//                rawResult is JSONArray -> {
//                    // 处理 JSONArray
//                    result = rawResult.getJSONObject(0)
//                }
//                else -> {
//                    // 其他情况
//                    null
//                }
//            }
            Log.i(TAG, "got $isFinal")
            val result = JSONObject(data).opt("result").let { rawResult ->
                when (rawResult) {
                    is JSONObject -> rawResult
                    is JSONArray -> rawResult.optJSONObject(0)
                    else -> null
                }
            }
            if (result == null) {
                return
            }

//            var text = result.getString("text")
//            var definite = utterances.getJSONObject(0).getBoolean("definite")
//            if (text.isEmpty()) {
//                return
//            }

            var utterances = result.getJSONArray("utterances")
            if (utterances == null || utterances.length() == 0) {
                return
            }
//            for (i in 0 until utterances.length()) {
//                val utterance = utterances.getString(i) // 或 getJSONObject(i) 根据实际内容
//                // 处理每个 utterance
//                Log.i(TAG, "$i: $utterance\n")
//            }
            if (utterances.length() > reportIndex) {
                reportIndex = utterances.length() - 1
            }
            var ret = utterances.getJSONObject(reportIndex - 1)
            var text = ret.getString("text")
            var definite = ret.getBoolean("definite")
            if (text.equals(lastAsrResult) && lastDefinite == definite) {
                return
            }
            lastAsrResult = text
            lastDefinite = definite
            reportIndex = utterances.length()
            ChatSDK.events().source()
                .accept(NetworkEvent.messageInputAsr(definite, text, true))

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
            var msg = data
            if (reader.has("err_msg")) {
                msg = reader.getString("err_msg")
            }
            ChatSDK.events().source().accept(NetworkEvent.errorEvent(null, "asr", msg))
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
        if (mSpeechEngine == null) {
            return
        }
        //【必需配置】Engine Name
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.ASR_ENGINE
        )

        //【可选配置】Debug & Log
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING,
            "/sdcard/Download/"
        )
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

        //一句话...
//        configBaseInitParams()

        //大模型...
        configLlmInitParams()

        //【可选配置】最大录音时长，默认60000ms，如果使用场景超过60s请修改该值，-1为不限制录音时长
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_VAD_MAX_SPEECH_DURATION_INT,
            180000
        );


        //【可选配置】更新 ASR 热词
//        if (!mSettings.getString(R.string.config_asr_hotwords).isEmpty()) {
//            Log.d(SpeechDemoDefines.TAG, "Set hotwords.");
//            setHotWords(mSettings.getString(R.string.config_asr_hotwords));
//        }


//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_ASR_REQ_PARAMS_STRING,params
//        );

//        mSpeechEngine.setOptionString(SpeechEngineDefines.PARAMS_KEY_ASR_REQ_PARAMS_STRING,"{\"force_to_speech_time\":0, \"end_window_size\":800,\"corpus\":{\"boosting_table_id\":\"热词id\"}, \"context\": \"{\"correct_words\": {\"deep seek\": \"DeepSeek\"}}\"}");


////        // scale为float类型参数，其中叠词的范围为[1.0,2.0]，非叠词的范围为[1.0,50.0]，scale值越大，结果中出现热词的概率越大
//        val hotWords = "{\"hotwords\":[{\"word\":\"属灵\",\"scale\":50.0},{\"word\":\"分别为圣\",\"scale\":50.0}]}"
//        mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_UPDATE_ASR_HOTWORDS, hotWords)

        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_ASR_REQ_PARAMS_STRING,"{\"force_to_speech_time\":0, \"end_window_size\":800,\"corpus\":{\"boosting_table_id\":\"ec86438f-22e4-41ba-a714-af4a7b874cbf\"}, \"context\": \"{\"correct_words\": {\"deep seek\": \"DeepSeek\"}}\"}");


        //【可选配置】在线请求的建连与接收超时，一般不需配置使用默认值即可
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_ASR_CONN_TIMEOUT_INT, 3000)
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_ASR_RECV_TIMEOUT_INT, 5000)

        //【可选配置】在线请求断连后，重连次数，默认值为0，如果需要开启需要设置大于0的次数
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_ASR_MAX_RETRY_TIMES_INT,
            2
        )
    }


    private fun configBaseInitParams() {
        //流式语音识别
        //https://www.volcengine.com/docs/6561/113642

        //【必需配置】识别服务域名
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_ADDRESS_STRING,
            "wss://openspeech.bytedance.com"
        )

        //【必需配置】识别服务Uri
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_URI_STRING,
            "/api/v2/asr"
        )

        //【必需配置】鉴权相关：Appid
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, "2617262954")

//        val token: String? = mSettings.getString(R.string.config_token)
        //【必需配置】鉴权相关：Token
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING,
            "Bearer;Yn2tI0wSDWOgEJhP8PPLcXtGInB11N4M"
        )

        //【必需配置】识别服务所用集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_CLUSTER_STRING,
            "volcengine_input_common"
        )

    }


    private fun configLlmInitParams() {
        //大模型流式语音识别
        //https://www.volcengine.com/docs/6561/1395846
        if (mSpeechEngine == null) {
            return
        }

        //【必需配置】识别服务域名，固定值
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_ADDRESS_STRING,
            "wss://openspeech.bytedance.com"
        );
//【必需配置】识别服务Uri，参考大模型流式语音识别API--简介
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ASR_URI_STRING,
            "/api/v3/sauc/bigmodel"
        );
//【必需配置】鉴权相关：Appid，参考控制台使用FAQ--Q1
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, "2617262954");
//【必需配置】鉴权相关：Token，无需添加Bearer; 前缀，参考控制台使用FAQ--Q1
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING,
            "Yn2tI0wSDWOgEJhP8PPLcXtGInB11N4M"
        );
//【必需配置】识别服务资源信息ResourceId，参考大模型流式语音识别API--鉴权
// https://www.volcengine.com/docs/6561/1354869
//        小时版：volc.bigasr.sauc.duration,并发版：volc.bigasr.sauc.concurrent
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_RESOURCE_ID_STRING,
            "volc.bigasr.sauc.duration"
        );
//【必需配置】协议类型，大模型流式识别协议需设置为Seed，
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_PROTOCOL_TYPE_INT,
            SpeechEngineDefines.PROTOCOL_TYPE_SEED
        );

    }

    private var recordIsRunning = false
    var executor: ExecutorService? = Executors.newFixedThreadPool(1)
    private var recordRunnable: Runnable? = null

    fun startAsr() {
        recordIsRunning = false
        recordRunnable = Runnable {

            lastAsrResult = ""
            lastDefinite = false
            reportIndex = 1
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
//            mSpeechEngine!!.setOptionString(
//                "result_type",
//                "single"
//            )
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_ASR_SHOW_UTTER_BOOL,
                true
            )

            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(TAG, "关闭引擎（同步）")
            Log.i(TAG, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                var msg = "directive DIRECTIVE_SYNC_STOP_ENGINE failed, $ret"
                Log.e(TAG, msg)
                ChatSDK.events().source().accept(NetworkEvent.errorEvent(null, "asr", msg))
            } else {
                Log.i(TAG, "启动引擎")
                Log.i(TAG, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret == SpeechEngineDefines.ERR_REC_CHECK_ENVIRONMENT_FAILED) {

                    var msg = "ERR_REC_CHECK_ENVIRONMENT_FAILED"
                    Log.e(TAG, msg)
                    ChatSDK.events().source().accept(NetworkEvent.errorEvent(null, "asr", msg))
//                    mEngineStatusTv.setText(R.string.check_rec_permission)
//                    requestPermission(com.bytedance.speech.speechdemo.AsrActivity.ASR_PERMISSIONS)
                } else if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    var msg = "send directive start failed, $ret"
                    Log.e(TAG, msg)
                    ChatSDK.events().source().accept(NetworkEvent.errorEvent(null, "asr", msg))
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