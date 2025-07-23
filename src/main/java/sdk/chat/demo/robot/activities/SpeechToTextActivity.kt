package sdk.chat.demo.robot.activities
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.handlers.SpeechToTextHelper
import sdk.chat.demo.robot.push.UpdateTokenWorker

class SpeechToTextActivity  : AppCompatActivity(), View.OnClickListener {
    private lateinit var speechToTextHelper: SpeechToTextHelper
    private lateinit var resultTextView: TextView
    private lateinit var tvTaskIndex: EditText
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

        // 检查并请求录音权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
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

            R.id.updateToken-> {
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
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit() {
                        putBoolean("has_shown_guide", false)
                    }

                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                if(threadHandler.welcome!=null){
                    ChatSDK.db().delete(threadHandler.welcome)
                }
                JsonCacheManager.save(this@SpeechToTextActivity,"gwTaskProcess","")
            }
            R.id.setTask -> {
                var taskIndex: Int = tvTaskIndex.text.toString().toInt()
                DailyTaskHandler.testTaskDetail(taskIndex)
            }
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}