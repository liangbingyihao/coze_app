package sdk.chat.demo.robot.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import materialsearchview.MaterialSearchView
import sdk.chat.core.dao.Thread
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.HistoryAdapter
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.listener.GWClickListener
import sdk.chat.ui.activities.BaseActivity
import java.util.Locale


class ChatActivity : BaseActivity(), View.OnClickListener,
    GWClickListener.TTSSpeaker {
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var sessions: List<Thread>
    private var currentSession: Thread? = null
    private lateinit var sessionAdapter: HistoryAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var textToSpeech: TextToSpeech? = null
    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, messageId: String? = null) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, messageId)
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        val messageId = intent.getStringExtra(EXTRA_INITIAL_DATA)

// 设置参数
        val fragment = GWChatFragment().apply {
            arguments = bundleOf(
                "KEY_MESSAGE_ID" to messageId,
            )
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, chatTag).commit()


        findViewById<View?>(R.id.home).setOnClickListener(this)

        // 检查 TTS 是否可用
        // 注册 ActivityResultLauncher
        ttsCheckLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    initTTS() // TTS 可用，初始化
                } else {
                    // 提示用户安装 TTS 数据
                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    startActivity(installIntent)
                }
            }

        // 检查 TTS 数据
        val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
//        ttsCheckLauncher.launch(checkIntent)


        if (checkIntent.resolveActivity(packageManager) != null) {
            // 确认有 TTS 引擎后再启动
            ttsCheckLauncher.launch(checkIntent)
        } else {
            // 设备完全无 TTS 支持时的处理
//            handleNoTtsEngine()
            Toast.makeText(this, "暂不支持语音播放", Toast.LENGTH_SHORT).show()

//            AlertDialog.Builder(this)
//                .setTitle("需要语音支持")
//                .setMessage("您的设备缺少语音合成引擎，是否安装 Google TTS？")
//                .setPositiveButton("安装") { _, _ ->
//                    safeInstallTtsEngine()
//                }
//                .setNegativeButton("取消") { _, _ ->
//                    Toast.makeText(this, "部分功能将不可用", Toast.LENGTH_SHORT).show()
//                }
//                .show()
        }
    }

    private fun safeInstallTtsEngine() {
        val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 再次检查安装 Intent 是否可用
        if (installIntent.resolveActivity(packageManager) != null) {
            startActivity(installIntent)
        } else {
            // 连安装入口都没有的极端情况（如国产 ROM）
            Toast.makeText(
                this,
                "您的设备不支持语音功能",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
                } else {
//                    speek("你好, Android TTS")
//                    textToSpeech.speak("Hello, Android TTS", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Toast.makeText(this@ChatActivity, "onStart", Toast.LENGTH_SHORT).show()
            }

            override fun onDone(utteranceId: String?) {

                //通知老的播放按键恢复一下
                threadHandler.setPlayingMsg(null);
                Toast.makeText(this@ChatActivity, "onDone", Toast.LENGTH_SHORT).show()
            }

            override fun onError(utteranceId: String?) {
                threadHandler.setPlayingMsg(null);
                Toast.makeText(this@ChatActivity, "playError", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun speek(text: String, msgId: String) {
        var rawText = text.replace("*", "").replace("<br>|</br>|<br/>".toRegex(), "")
        textToSpeech?.language = LanguageUtils.getTextLanguage(rawText)
        textToSpeech?.speak(rawText, TextToSpeech.QUEUE_FLUSH, null, msgId)
    }

    override fun getCurrentUtteranceId(): String? {
        return currentUtteranceId;
    }

    override fun stop() {
        if (textToSpeech!=null&& textToSpeech!!.isSpeaking) {
            textToSpeech!!.stop()
        }
    }


    override fun getLayout(): Int {
        return R.layout.activity_chat;
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.home) {
            finish();
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(textToSpeech!=null){
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
    }
}