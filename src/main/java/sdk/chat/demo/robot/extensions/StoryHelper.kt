package sdk.chat.demo.robot.extensions

import android.graphics.drawable.Drawable
import android.view.View
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.lang.reflect.Method

object StoryHelper {
    private var methodSetBackground: Method? = null
    private var methodGetDate: Method? = null

    fun getCalendarDate(v: View): CalendarDay? {
        if (methodGetDate == null) {
            methodGetDate = v.javaClass.getMethod(
                "getDate"
            )
        }
        return methodGetDate?.invoke(v) as CalendarDay?
    }

    fun setCustomBackground(v: View, d: Drawable?) {
        if (methodSetBackground == null) {
            methodSetBackground = v.javaClass.getMethod(
                "setCustomBackground",
                Drawable::class.java
            )
        }
        methodSetBackground?.invoke(v, d)
    }

//    fun getChapterByDate(history:TaskHistory,d:CalendarDay):Chapter{
//        return history.history.filter { it.calendarDay ==d  }
//    }
}