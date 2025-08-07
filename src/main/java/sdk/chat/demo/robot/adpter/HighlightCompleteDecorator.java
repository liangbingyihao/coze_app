package sdk.chat.demo.robot.adpter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.Collection;
import java.util.HashSet;

/**
 * Highlight Saturdays and Sundays with a background
 */
public class HighlightCompleteDecorator implements DayViewDecorator {

    private final Drawable highlightDrawable;
    private HashSet<CalendarDay> dates;
    private static final int color = Color.parseColor("#228BC34A");

    public HighlightCompleteDecorator(Collection<CalendarDay> dates,Drawable highlightDrawable) {
        this.highlightDrawable = highlightDrawable;
        this.dates = new HashSet<>(dates);
    }

    @Override
    public boolean shouldDecorate(final CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(final DayViewFacade view) {
        view.setBackgroundDrawable(highlightDrawable);
    }
}
