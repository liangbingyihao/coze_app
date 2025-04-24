package sdk.chat.demo.robot.ui

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.drawerlayout.widget.DrawerLayout

object KeyboardDrawerHelper {
    fun setup(drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset > 0.3) {
                    getInputMethodManager(drawerView.context)?.let { imm ->
                        imm.hideSoftInputFromWindow(drawerView.windowToken,
                            InputMethodManager.HIDE_NOT_ALWAYS)
                    }
                }
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
    }

    private fun getInputMethodManager(context: Context): InputMethodManager? {
        return context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }
}