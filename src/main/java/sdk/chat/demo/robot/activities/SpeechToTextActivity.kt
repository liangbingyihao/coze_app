package sdk.chat.demo.robot.activities
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import sdk.chat.demo.pre.R
import android.content.pm.PackageManager
import sdk.chat.demo.robot.handlers.SpeechToTextHelper

class SpeechToTextActivity  : AppCompatActivity() {
    private lateinit var speechToTextHelper: SpeechToTextHelper
    private lateinit var resultTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        resultTextView = findViewById(R.id.resultTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

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

        startButton.setOnClickListener {
            speechToTextHelper.startListening()
        }

        stopButton.setOnClickListener {
            speechToTextHelper.stopListening()
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

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}