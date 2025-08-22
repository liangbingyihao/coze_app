package sdk.chat.demo.robot.audio

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngine.SpeechListener
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import org.json.JSONException
import org.json.JSONObject
import sdk.chat.core.dao.Message
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.utils.AppBackgroundMonitor.StopListener
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.Locale

object TTSHelper {
    private val TAG = "TTSHelper"
    private val ttsType = 1
    private var textToSpeech: TextToSpeech? = null
    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>
    private var mCurTtsText = ""
    private var playingMsg: Message? = null

    //doubao
    private var mSpeechEngine: SpeechEngine? = null

    // Options Default Value
    private const val mTtsSilenceDuration = 0
    private const val mTtsSpeakSpeed = 10
    private const val mTtsAudioVolume = 10
    private const val mTtsAudioPitch = 10

    // Novel Scenario Related
    private var mTtsSynthesisFromPlayer = false
    private const val mTtsPlayingProgress = 0.0
    private var mTtsPlayingIndex = -1
    private var mTtsSynthesisIndex = 0
    private var mTtsSynthesisText: MutableList<String> = mutableListOf()
    private var mTtsSynthesisMap: MutableMap<String?, Int?>? = mutableMapOf()

    // Engine State
    private var mEngineInited = false
    private var mConnectionCreated = false
    private var mEngineStarted = false
    private var mPlayerPaused = false
    private var mCurTtsWorkMode = SpeechEngineDefines.TTS_WORK_MODE_ONLINE
    private var mAudioManager: AudioManager? = null
    private var mResumeOnFocusGain = true
    private var mPlaybackNowAuthorized = false
    private var audioFocusRequest: AudioFocusRequest? = null
    var voiceType = "BV026_streaming"


    fun initTTS(context: AppCompatActivity) {
        // 检查 TTS 是否可用
        // 注册 ActivityResultLauncher

        voiceType =
            context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("db_voice_type", "BV026_streaming")
                .toString()

        ttsCheckLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    initSystemTTS(context) // TTS 可用，初始化
                } else {
                    // 提示用户安装 TTS 数据
                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    context.startActivity(installIntent)
                }
            }

        // 检查 TTS 数据
        val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
//        ttsCheckLauncher.launch(checkIntent)


        if (checkIntent.resolveActivity(context.packageManager) != null) {
            // 确认有 TTS 引擎后再启动
            ttsCheckLauncher.launch(checkIntent)
        } else {
            // 设备完全无 TTS 支持时的处理
//            handleNoTtsEngine()
            Toast.makeText(context, "暂不支持语音播放", Toast.LENGTH_SHORT).show()
        }

        initDoubaoTTS(context)
    }

    private fun initSystemTTS(context: AppCompatActivity) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show()
                } else {
//                    speek("你好, Android TTS")
//                    textToSpeech.speak("Hello, Android TTS", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Toast.makeText(context, "onStart", Toast.LENGTH_SHORT).show()
            }

            override fun onDone(utteranceId: String?) {

                //通知老的播放按键恢复一下
                setPlayingMsg(null)
                Toast.makeText(context, "onDone", Toast.LENGTH_SHORT).show()
            }

            override fun onError(utteranceId: String?) {
                setPlayingMsg(null)
                Toast.makeText(context, "playError", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun speek(text: String, msgId: String) {
        if (ttsType == 0) {
            mCurTtsText = text.replace("*", "").replace("<br>|</br>|<br/>".toRegex(), "")
            textToSpeech?.language = LanguageUtils.getTextLanguage(mCurTtsText)
            textToSpeech?.speak(mCurTtsText, TextToSpeech.QUEUE_FLUSH, null, msgId)
        } else {
//            mCurTtsText = mCurTtsText.take(20)
            mCurTtsText = text
            startEngine()
            triggerSynthesis()
        }
    }

    fun stop() {
        if (ttsType == 0) {
            if (textToSpeech != null && textToSpeech!!.isSpeaking) {
                textToSpeech!!.stop()
                setPlayingMsg(null)
            }
        } else {
            controlPlayingStatus()
        }
    }

    fun clear() {
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }

        if (mSpeechEngine != null) {
            Log.i(TAG, "引擎析构.");
            mSpeechEngine!!.destroyEngine();
            mSpeechEngine = null;
            Log.i(TAG, "引擎析构完成!");
        }
    }

    //doubao

    private fun initDoubaoTTS(context: AppCompatActivity) {
        initEngineInternal()
        mAudioManager =
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        ChatSDK.appBackgroundMonitor().addListener(mBackgroundListener);
    }

    private var mBackgroundListener: StopListener =
        object : StopListener {
            override fun didStop() {
                pausePlayback()
            }

        }

    private var mAFChangeListener: OnAudioFocusChangeListener =
        object : OnAudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: Int) {
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(
                            TAG,
                            "onAudioFocusChange: AUDIOFOCUS_GAIN, $mResumeOnFocusGain"
                        )
                        if (mResumeOnFocusGain) {
                            mResumeOnFocusGain = false
                            resumePlayback()
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS")
                        mResumeOnFocusGain = false
                        pausePlayback()
                        mPlaybackNowAuthorized = false
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT")
                        mResumeOnFocusGain = mEngineStarted
                        pausePlayback()
                    }
                }
            }

        }

    private fun pausePlayback() {
        Log.i(TAG, "暂停播放")
        Log.i(TAG, "Directive: DIRECTIVE_PAUSE_PLAYER")
        val ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_PAUSE_PLAYER, "")
        if (ret == SpeechEngineDefines.ERR_NO_ERROR) {
            mPlayerPaused = true
        }
        Log.d(TAG, "Pause playback status:" + ret)
    }

    private fun resumePlayback() {
        Log.i(TAG, "继续播放")
        Log.i(TAG, "Directive: DIRECTIVE_RESUME_PLAYER")
        val ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_RESUME_PLAYER, "")
        if (ret == SpeechEngineDefines.ERR_NO_ERROR) {
            mPlayerPaused = false
        }
        Log.d(TAG, "Resume playback status:" + ret)
    }

    private fun controlPlayingStatus() {
        Log.d(
            TAG,
            "Pause or resume player, current player status: $mPlayerPaused"
        )
        if (mPlayerPaused) {
            if (!mPlaybackNowAuthorized) { // AudioFocus 被其他 APP 占用，需要再次获取
                AcquireAudioFocus()
            }
            resumePlayback()
        } else {
            pausePlayback()
        }
        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(this.playingMsg))
    }

    private val mSpeechListener: SpeechListener = object : SpeechListener {
        override fun onSpeechMessage(type: Int, data: ByteArray, len: Int) {
            val stdData = String(data)

            when (type) {
                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                    // Callback: 引擎启动成功回调
                    Log.i(TAG, "Callback: 引擎启动成功: data: " + stdData)
                    mEngineStarted = true
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                    // Callback: 引擎关闭回调
                    Log.i(TAG, "Callback: 引擎关闭: data: " + stdData)
                    mEngineStarted = false
                    mConnectionCreated = false
                    mPlayerPaused = false
                    // Abandon audio focus when playback complete
                    if (mPlaybackNowAuthorized) {
                        mAudioManager?.abandonAudioFocus(mAFChangeListener);
                        mPlaybackNowAuthorized = false;
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                    // Callback: 错误信息回调
                    Log.e(TAG, "Callback: 错误信息: " + stdData)
//                    setPlayingMsg(null)
                    speechError(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_BEGIN -> {
                    // Callback: 合成开始回调
                    Log.e(TAG, "Callback: 合成开始: " + stdData)
                    updateSynthesisMap(stdData)
//                    speechStartSynthesis(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_END -> {
                    // Callback: 合成结束回调
                    Log.e(TAG, "Callback: 合成结束: " + stdData)
                    speechFinishSynthesis(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_START_PLAYING -> {
                    // Callback: 播放开始回调
                    Log.e(TAG, "Callback: 播放开始: " + stdData)
//                    speechStartPlaying(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS -> {
                    // Callback: 播放进度回调
//                    Log.e(TAG, "Callback: 播放进度: " + stdData)
//                    speechPlayingProgress(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_FINISH_PLAYING -> {
                    // Callback: 播放结束回调
                    Log.e(TAG, "Callback: 播放结束: " + stdData)
                    speechFinishPlaying(stdData)
                }

                SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA -> {
                    // Callback: 音频数据回调
                    Log.e(
                        TAG,
                        String.format("Callback: 音频数据，长度 %d 字节", stdData.length)
                    )
                }

                else -> {}
            }
        }
    }

    fun speechError(data: String) {
        try {
            val reader = JSONObject(data)
            if (!reader.has("err_code") || !reader.has("err_msg")) {
                return
            }
            val code = reader.getInt("err_code")
            when (code) {
                SpeechEngineDefines.CODE_TTS_LIMIT_QPS, SpeechEngineDefines.CODE_TTS_LIMIT_COUNT, SpeechEngineDefines.CODE_TTS_SERVER_BUSY, SpeechEngineDefines.CODE_TTS_LONG_TEXT, SpeechEngineDefines.CODE_TTS_INVALID_TEXT, SpeechEngineDefines.CODE_TTS_SYNTHESIS_TIMEOUT, SpeechEngineDefines.CODE_TTS_SYNTHESIS_ERROR, SpeechEngineDefines.CODE_TTS_SYNTHESIS_WAITING_TIMEOUT, SpeechEngineDefines.CODE_TTS_ERROR_UNKNOWN -> {
                    Log.w(
                        TAG,
                        "When meeting this kind of error, continue to synthesize."
                    )
                    synthesisNextSentence()
                }

//                else -> setResultText(data)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun speechFinishSynthesis(data: String?) {
        synthesisNextSentence()
    }

    fun speechFinishPlaying(data: String?) {
        mTtsSynthesisMap!!.remove(data)
        if (mTtsSynthesisMap!!.isEmpty()) {
            setPlayingMsg(null)
        } else {
            if (mTtsSynthesisFromPlayer) {
                triggerSynthesis()
                mTtsSynthesisFromPlayer = false
            }

        }
    }

    private fun updateSynthesisMap(synthesisId: String?) {
        if (mTtsSynthesisIndex < mTtsSynthesisText.size) {
            mTtsSynthesisMap!!.put(synthesisId, mTtsSynthesisIndex)
        }
    }

    private fun synthesisNextSentence() {
        if (mEngineStarted) {
            ++mTtsSynthesisIndex
            if (mTtsSynthesisIndex < mTtsSynthesisText.size) {
                triggerSynthesis()
            }
        }
    }

    private fun triggerSynthesis() {
        configSynthesisParams()
        // DIRECTIVE_SYNTHESIS 是连续合成必需的一个指令，在成功调用 DIRECTIVE_START_ENGINE 之后，每次合成新的文本需要再调用 DIRECTIVE_SYNTHESIS 指令
        // DIRECTIVE_SYNTHESIS 需要在当前没有正在合成的文本时才可以成功调用，否则就会报错 -901，可以在收到 MESSAGE_TYPE_TTS_SYNTHESIS_END 之后调用
        // 当使用 SDK 内置的播放器时，为了避免缓存过多的音频导致内存占用过高，SDK 内部限制缓存的音频数量不超过 5 次合成的结果，
        // 如果 DIRECTIVE_SYNTHESIS 后返回 -902, 就需要在下一次收到 MESSAGE_TYPE_TTS_FINISH_PLAYING 再去调用 MESSAGE_TYPE_TTS_FINISH_PLAYING
        Log.i(TAG, "触发合成")
        Log.i(TAG, "Directive: DIRECTIVE_SYNTHESIS")
        val ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNTHESIS, "")
        if (ret != 0) {
            Log.e(TAG, "Synthesis faile: $ret")
            if (ret == SpeechEngineDefines.ERR_SYNTHESIS_PLAYER_IS_BUSY) {
                mTtsSynthesisFromPlayer = true
            }
        }
    }

    private fun resetTtsContext() {
        mTtsPlayingIndex = -1
        mTtsSynthesisIndex = 0
        mTtsSynthesisFromPlayer = false
        mTtsSynthesisText.clear()
        mTtsSynthesisMap?.clear()
    }

    private fun prepareTextList(): Boolean {
        resetTtsContext()

//        var ttsText = playingMsg.getf
//        if (ttsText.isEmpty()) {
//            ttsText =
//                "愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。能做事的做事，能发声的发声。有一分热，发一分光。就令萤火一般，也可以在黑暗里发一点光，不必等候炬火。此后如竟没有炬火：我便是唯一的光。"
//        }

        //【必需配置】需合成的文本，不可超过 80 字
        if (mTtsSynthesisText == null || mTtsSynthesisText.isEmpty()) {
            // 使用下面几个标点符号来分句，会让通过 MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS 返回的播放进度更加准确
            val tmp: Array<String?> =
                mCurTtsText.split("[;|!|?|。|！|？|；|…|,|.]".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (j in tmp.indices) {
                AddSentence(tmp[j].toString())
            }
        }
        Log.d(TAG, "Synthesis text item num: " + mTtsSynthesisText!!.size)
        return !mTtsSynthesisText.isEmpty()
    }

    private fun AddSentence(text: String) {
        val tmp = text.trim { it <= ' ' }
        if (!tmp.isEmpty()) {
            mTtsSynthesisText.add(tmp)
        }
    }

    private fun initEngineInternal() {
        var ret = SpeechEngineDefines.ERR_NO_ERROR
        if (mSpeechEngine == null) {
            mSpeechEngine = SpeechEngineGenerator.getInstance()
            mSpeechEngine!!.createEngine()
        }
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            mEngineInited = false;
            return
        }
        configInitParams()

        val startInitTimestamp = System.currentTimeMillis()
        ret = mSpeechEngine!!.initEngine()
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            val errMessage = "初始化失败，返回值: " + ret
            Log.e(TAG, errMessage)
            mEngineInited = false;
            return
        }
        Log.i(TAG, "设置消息监听")
        mSpeechEngine!!.setListener(mSpeechListener)

        val cost = System.currentTimeMillis() - startInitTimestamp
        Log.d(TAG, String.format("初始化耗时 %d 毫秒", cost))
        mEngineInited = true;
    }

    private fun startEngine() {
        Log.d(TAG, "Start engine, current status: " + mEngineStarted)
        if (true || !mEngineStarted) {
            AcquireAudioFocus()
            if (!mPlaybackNowAuthorized) {
                Log.w(TAG, "Acquire audio focus failed, can't play audio")
                return
            }

            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(TAG, "关闭引擎（同步）")
            Log.i(TAG, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(TAG, "send directive syncstop failed, " + ret)
            } else {
                configStartTtsParams()
                Log.i(TAG, "启动引擎")
                Log.i(TAG, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    Log.e(TAG, "send directive start failed, " + ret)
                }
            }
        }
    }

    private fun configStartTtsParams() {
        //【必需配置】TTS 使用场景
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_SCENARIO_STRING,
            SpeechEngineDefines.TTS_SCENARIO_TYPE_NOVEL
//            SpeechEngineDefines.TTS_SCENARIO_TYPE_NORMAL
        )

        // 准备待合成的文本
        if (!prepareTextList()) {
            speechError("{err_code:3006, err_msg:\"Invalid input text.\"}")
            return
        }

//
//        //【可选配置】是否使用 SDK 内置播放器播放合成出的音频，默认为 true
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL,
//            mSettings.getBoolean(R.string.config_tts_player)
//        )
//        //【可选配置】是否令 SDK 通过回调返回合成的音频数据，默认不返回。
//        // 开启后，SDK 会流式返回音频，收到 MESSAGE_TYPE_TTS_AUDIO_DATA_END 回调表示当次合成所有的音频已经全部返回
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_TTS_DATA_CALLBACK_MODE_INT,
//            if (mSettings.getBoolean(R.string.config_tts_data_callback)) 2 else 0
//        )
    }

    private fun configSynthesisParams() {

        val text: String? = mTtsSynthesisText[mTtsSynthesisIndex]
        Log.e(TAG, "Synthesis Text: $text")
        //【必需配置】需合成的文本，不可超过 80 字
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_STRING, text)
//        //【可选配置】需合成的文本的类型，支持直接传文本(TTS_TEXT_TYPE_PLAIN)和传 SSML 形式(TTS_TEXT_TYPE_SSML)的文本
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_TYPE_STRING,
//            mTtsTextTypeArray[mSettings.getOptions(R.string.tts_text_type_title).chooseIdx]
//        )
//        mTtsSpeakSpeed = mSettings.getInt(R.string.config_tts_speak_speed)
//        //【可选配置】用于控制 TTS 音频的语速，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_SPEED_INT, mTtsSpeakSpeed)
//        mTtsAudioVolume = mSettings.getInt(R.string.config_tts_audio_volume)
//        //【可选配置】用于控制 TTS 音频的音量，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_VOLUME_INT, mTtsAudioVolume)
//        mTtsAudioPitch = mSettings.getInt(R.string.config_tts_audio_pitch)
//        //【可选配置】用于控制 TTS 音频的音高，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_PITCH_INT, mTtsAudioPitch)
//        mTtsSilenceDuration = mSettings.getInt(R.string.config_tts_silence_duration)
//        //【可选配置】是否在文本的每句结尾处添加静音段，单位：毫秒，默认为 0ms
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_SILENCE_DURATION_INT,
            mTtsSilenceDuration
        )

        // ------------------------ 在线合成相关配置 -----------------------
//        var curVoiceOnline: String = mSettings.getString(R.string.config_voice_online)
//        if (curVoiceOnline.isEmpty()) {
//            curVoiceOnline = mSettings.getOptionsValue(R.string.config_voice_online)
//        }
//        mCurVoiceOnline = curVoiceOnline
//        mCurVoiceOnline = "volc.megatts.default"
//        Log.d(SpeechDemoDefines.TAG, "Current online voice: " + mCurVoiceOnline)
        //【必需配置】在线合成使用的发音人代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING,
            "volc.megatts.default"
        )
//        var curVoiceTypeOnline: String = mSettings.getString(R.string.config_voice_type_online)
//        if (curVoiceTypeOnline.isEmpty()) {
//            curVoiceTypeOnline = mSettings.getOptionsValue(R.string.config_voice_type_online)
//        }
//        mCurVoiceTypeOnline = curVoiceTypeOnline
//        mCurVoiceTypeOnline = "BV026_streaming"
//        Log.d(TAG, "Current online voice type: " + mCurVoiceTypeOnline)
        //【必需配置】在线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_ONLINE_STRING,
            voiceType
        )

        //【可选配置】是否打开在线合成的服务端缓存，默认关闭
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_CACHE_BOOL,
            true
        )
//        //【可选配置】指定在线合成的语种，默认为空，即不指定
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_TTS_LANGUAGE_ONLINE_STRING,
//            mSettings.getString(R.string.config_tts_language_online)
//        )
//        //【可选配置】是否启用在线合成的情感预测功能
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_TTS_WITH_INTENT_BOOL,
//            mSettings.getBoolean(R.string.config_tts_with_intent)
//        )
//        //【可选配置】指定在线合成的情感，例如 happy, sad 等
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_TTS_EMOTION_STRING,
//            mSettings.getString(R.string.config_tts_emotion)
//        )
//        //【可选配置】需要返回详细的播放进度时应配置为 1, 否则配置为 0 或不配置
//        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_WITH_FRONTEND_INT, 1)
//        //【可选配置】使用复刻音色
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_TTS_USE_VOICECLONE_BOOL,
//            mSettings.getBoolean(R.string.config_tts_use_voiceclone)
//        )
//        //【可选配置】在开启前述使用复刻音色的开关后，制定复刻音色所用的后端集群
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_TTS_BACKEND_CLUSTER_STRING,
//            mSettings.getString(R.string.config_backend_cluster)
//        )


    }

    private fun AcquireAudioFocus() {
//AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE or AUDIOFOCUS_GAIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(mAFChangeListener)
                    .build()

            audioFocusRequest = focusRequest
            val result = mAudioManager?.requestAudioFocus(focusRequest)

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlaybackNowAuthorized = true
            } else {
                mPlaybackNowAuthorized = false
            }
        } else {
            // 兼容旧版本
            val result = mAudioManager?.requestAudioFocus(
                mAFChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlaybackNowAuthorized = true
            } else {
                mPlaybackNowAuthorized = false
            }
        }
    }


    private fun configInitParams() {
        //【必需配置】Engine Name
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.TTS_ENGINE
        )

        //【必需配置】Work Mode, 可选值如下
        // SpeechEngineDefines.TTS_WORK_MODE_ONLINE, 只进行在线合成，不需要配置离线合成相关参数；
        // SpeechEngineDefines.TTS_WORK_MODE_OFFLINE, 只进行离线合成，不需要配置在线合成相关参数；
        // SpeechEngineDefines.TTS_WORK_MODE_BOTH, 同时发起在线合成与离线合成，在线请求失败的情况下，使用离线合成数据，该模式会消耗更多系统性能；
        // SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE, 先发起在线合成，失败后（网络超时），启动离线合成引擎开始合成；
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_WORK_MODE_INT,
            mCurTtsWorkMode
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
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_DEVICE_ID_STRING,
//            SensitiveDefines.DID
//        )

//        //【可选配置】是否将合成出的音频保存到设备上，为 true 时需要正确配置 PARAMS_KEY_TTS_AUDIO_PATH_STRING 才会生效
//        mSpeechEngine!!.setOptionBoolean(
//            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_DUMP_BOOL,
//            mSettings.getBoolean(R.string.config_tts_dump)
//        )
//        // TTS 音频文件保存目录，必须在合成之前创建好且 APP 具有访问权限，保存的音频文件名格式为 tts_{reqid}.wav, {reqid} 是本次合成的请求 id
//        // PARAMS_KEY_TTS_ENABLE_DUMP_BOOL 配置为 true 的音频时为【必需配置】，否则为【可选配置】
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_TTS_AUDIO_PATH_STRING,
//            mDebugPath
//        )

//        //【可选配置】合成出的音频的采样率，默认为 24000
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_TTS_SAMPLE_RATE_INT,
//            mSettings.getInt(R.string.config_tts_sample_rate)
//        )
//        //【可选配置】打断播放时使用多长时间淡出停止，单位：毫秒。默认值 0 表示不淡出
//        mSpeechEngine!!.setOptionInt(
//            SpeechEngineDefines.PARAMS_KEY_AUDIO_FADEOUT_DURATION_INT,
//            mSettings.getInt(R.string.config_audio_fadeout_duration)
//        )

        // ------------------------ 在线合成相关配置 -----------------------
//        mCurAppId = mSettings.getString(R.string.config_app_id)
//        if (mCurAppId.isEmpty()) {
//            mCurAppId = SensitiveDefines.APPID
//        }
        //【必需配置】在线合成鉴权相关：Appid
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, "2617262954")

//        var token: String = mSettings.getString(R.string.config_token)
//        if (token.isEmpty()) {
//            token = SensitiveDefines.TOKEN
//        }
        //【必需配置】在线合成鉴权相关：Token
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING,
            "Bearer;Yn2tI0wSDWOgEJhP8PPLcXtGInB11N4M"
        )

//        var address: String = mSettings.getString(R.string.config_address)
//        if (address.isEmpty()) {
//            address = SensitiveDefines.DEFAULT_ADDRESS
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current address: " + address)
        //【必需配置】语音合成服务域名
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_ADDRESS_STRING,
            "wss://openspeech.bytedance.com"
        )

//        var uri: String = mSettings.getString(R.string.config_uri)
//        if (uri.isEmpty()) {
//            uri = SensitiveDefines.TTS_DEFAULT_URI
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current uri: " + uri)
        //【必需配置】语音合成服务Uri
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_URI_STRING,
            "/api/v1/tts/ws_binary"
        )

//        var cluster: String = mSettings.getString(R.string.config_cluster)
//        if (cluster.isEmpty()) {
//            cluster = SensitiveDefines.TTS_DEFAULT_CLUSTER
//        }
//        Log.i(SpeechDemoDefines.TAG, "Current cluster: " + cluster)
        //【必需配置】语音合成服务所用集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_CLUSTER_STRING,
            "volcano_tts"
        )

        //【可选配置】在线合成下发的 opus-ogg 音频的压缩倍率
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_COMPRESSION_RATE_INT, 10)

//        // ------------------------ 离线合成相关配置 -----------------------
//        if (mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_ONLINE && mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_FILE) {
//            var ttsResourcePath: String? = ""
//            val resourceManager = SpeechResourceManagerGenerator.getInstance()
//            if (mSettings.getOptionsValue(
//                    R.string.tts_offline_resource_format_title,
//                    this
//                ) == "MultipleVoice"
//            ) {
//                ttsResourcePath =
//                    resourceManager.getResourcePath(mSettings.getString(R.string.config_tts_model_name))
//            } else if (mSettings.getOptionsValue(
//                    R.string.tts_offline_resource_format_title,
//                    this
//                ) == "SingleVoice"
//            ) {
//                ttsResourcePath = resourceManager.getResourcePath()
//            }
//            Log.d(SpeechDemoDefines.TAG, "tts resource root path:" + ttsResourcePath)
//            //【必需配置】离线合成所需资源存放路径
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_TTS_OFFLINE_RESOURCE_PATH_STRING,
//                ttsResourcePath
//            )
//        }

//        //【必需配置】离线合成鉴权相关：证书文件存放路径
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_LICENSE_DIRECTORY_STRING,
//            mDebugPath
//        )
//        val curAuthenticateType: String = mAuthenticationTypeArray[mSettings
//            .getOptions(R.string.config_authenticate_type).chooseIdx]
//        //【必需配置】Authenticate Type
//        mSpeechEngine!!.setOptionString(
//            SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_TYPE_STRING,
//            curAuthenticateType
//        )
//        if (curAuthenticateType == SpeechEngineDefines.AUTHENTICATE_TYPE_PRE_BIND) {
//            // 按包名授权，获取到授权的 APP 可以不限次数、不限设备数的使用离线合成
//            val ttsLicenseName: String = mSettings.getString(R.string.config_license_name)
//            val ttsLicenseBusiId: String = mSettings.getString(R.string.config_license_busi_id)
//
//            // 证书名和业务 ID, 离线合成鉴权相关，使用火山提供的证书下发服务时为【必需配置】, 否则为【无需配置】
//            // 证书名，用于下载按报名授权的证书文件
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_LICENSE_NAME_STRING,
//                ttsLicenseName
//            )
//            // 业务 ID, 用于下载按报名授权的证书文件
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_LICENSE_BUSI_ID_STRING,
//                ttsLicenseBusiId
//            )
//        } else if (curAuthenticateType == SpeechEngineDefines.AUTHENTICATE_TYPE_LATE_BIND) {
//            // 按装机量授权，不限制 APP 的包名和使用次数，但是限制使用离线合成的设备数量
//            //【必需配置】离线合成鉴权相关：Authenticate Address
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_ADDRESS_STRING,
//                SensitiveDefines.AUTHENTICATE_ADDRESS
//            )
//            //【必需配置】离线合成鉴权相关：Authenticate Uri
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_URI_STRING,
//                SensitiveDefines.AUTHENTICATE_URI
//            )
//            val businessKey: String = mSettings.getString(R.string.config_business_key)
//            val authenticateSecret: String =
//                mSettings.getString(R.string.config_authenticate_secret)
//            //【必需配置】离线合成鉴权相关：Business Key
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_BUSINESS_KEY_STRING,
//                businessKey
//            )
//            //【必需配置】离线合成鉴权相关：Authenticate Secret
//            mSpeechEngine!!.setOptionString(
//                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_SECRET_STRING,
//                authenticateSecret
//            )
//        }
//
//        // ------------------------ 在离线切换相关配置 -----------------------
//        if (mCurTtsWorkMode == SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE) {
//            // 断点续播功能在断点处会发生由在线合成音频切换到离线合成音频，为了提升用户体验，SDK 支持
//            // 淡出地停止播放在线音频然后再淡入地开始播放离线音频，下面两个参数可以控制淡出淡入的长度
//
//            //【可选配置】断点续播专用，切换到离线合成时淡入的音频长度，单位：毫秒
//
//            mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_FADEIN_DURATION_INT, 30)
//            //【可选配置】断点续播专用，在线合成停止播放时淡出的音频长度，单位：毫秒
//            mSpeechEngine!!.setOptionInt(
//                SpeechEngineDefines.PARAMS_KEY_TTS_FADEOUT_DURATION_INT,
//                30
//            )
//        }
    }


    fun getPlayingMsg(): Message? {
        return playingMsg
    }

    fun isPlayerPaused(): Boolean {
        return mPlayerPaused
    }

    fun setPlayingMsg(newPlaying: Message?): Boolean {
        if (this.playingMsg == null) {
            if (newPlaying != null) {
                this.playingMsg = newPlaying
            } else {
                return false
            }
        } else {
            val oldPlaying: Message? = this.playingMsg
            if (newPlaying != null && oldPlaying?.id == newPlaying.id) {
                return true
            } else {
                this.playingMsg = newPlaying
            }
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(oldPlaying))
        }
        if (newPlaying != null) {
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(newPlaying))
        }
        return true
    }
}