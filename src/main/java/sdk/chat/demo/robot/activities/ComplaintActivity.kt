package sdk.chat.demo.robot.activities

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.ui.utils.ToastHelper


class ComplaintActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvSubmit: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.submit).setOnClickListener(this)
        tvSubmit = findViewById<TextView>(R.id.messageInput)
        tvSubmit.requestFocus()
        tvSubmit.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(tvSubmit, InputMethodManager.SHOW_IMPLICIT)
        }, 500)
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

            R.id.submit -> {
                onSubmit()
            }

        }
    }


    fun onSubmit() {

        dm.add(
            LogUploader.uploadLogs(this, "feedback", tvSubmit.text.toString())
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { exportInfo ->
                        ToastHelper.show(
                            this,
                            R.string.submit_success
                        )
                        hideKeyboard()
                    },
                    { error ->
                        ToastHelper.show(
                            this,
                            error.message
                        )
                    }
                )
        )

    }

}