package sdk.chat.demo.robot.adpter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import sdk.chat.demo.pre.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> items;
    private int selectedPosition = -1;

    public CustomSpinnerAdapter(Context context, int resource, List<String> items) {
        super(context, resource, items);
        this.context = context;
        this.items = items;
    }

    // 正常状态下的视图
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.custom_spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        textView.setText(items.get(position));

        // 可以在这里设置选中状态的样式
        if (position == selectedPosition) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        } else {
            textView.setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary));
        }

        return convertView;
    }

    // 下拉菜单的视图
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.custom_spinner_dropdown_item, parent, false);
        }

        CheckedTextView textView = convertView.findViewById(R.id.spinner_dropdown_item_text);
        textView.setText(items.get(position));

        // 设置选中状态
        textView.setChecked(position == selectedPosition);

        return convertView;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
}