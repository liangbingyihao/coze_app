package sdk.chat.demo.robot.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import sdk.chat.demo.pre.R

class DialogEditSingle (
    context: Context,
    private val onSendClick: (String) -> Unit
) : Dialog(context, R.style.FullScreenDialog) {
    private lateinit var inputEditText: EditText

    override fun show() {
        super.show()
        inputEditText.postDelayed({
            inputEditText.requestFocus()
            showKeyboard()
        }, 200)
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun setEditDefault(title:String){
        inputEditText.setText(title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_edit_single)

        // 设置全屏
        window?.let {
            it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        inputEditText = findViewById<EditText>(R.id.edSummary)
        val sendButton = findViewById<TextView>(R.id.edSummaryConfirm)

        sendButton.setOnClickListener {
            val inputText = inputEditText.text?.toString()?.trim() ?: ""
            if (inputText.isNotEmpty()) {
                onSendClick(inputText)
                dismiss()
            } else {
                inputEditText.error = "请输入内容"
            }
        }

        findViewById<View>(R.id.edContainer).setOnClickListener {
            dismiss()
        }
    }
}