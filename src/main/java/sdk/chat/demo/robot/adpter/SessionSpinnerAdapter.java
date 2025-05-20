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
import sdk.chat.demo.robot.adpter.data.ArticleSession;

public class SessionSpinnerAdapter extends ArrayAdapter<ArticleSession> {

    private Context context;
    private int selectedPosition = -1;

    public SessionSpinnerAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
    }

    // 正常状态下的视图
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_session_spinner, parent, false);
        }

        ArticleSession session = getItem(position);
        if(session!=null){
            TextView textView = convertView.findViewById(R.id.spinner_item_text);
            textView.setText(session.getTitle());
        }

//        // 可以在这里设置选中状态的样式
//        if (position == selectedPosition) {
//            textView.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
//        } else {
//            textView.setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary));
//        }

        return convertView;
    }

    // 下拉菜单的视图
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_session_spinner_dropdown, parent, false);
        }
        ArticleSession session = getItem(position);
        if(session!=null){
            TextView textView = convertView.findViewById(R.id.spinner_dropdown_item_text);
            textView.setText(session.getTitle());
            // 设置选中状态
            if (position == selectedPosition) {
                textView.setTextColor(ContextCompat.getColor(context, R.color.item_text_selected));
            } else {
                textView.setTextColor(ContextCompat.getColor(context, R.color.item_text_normal));
            }
        }



        return convertView;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }


    public void setItems(List<ArticleSession> newItems) {
        this.clear();
        this.addAll(newItems);
        notifyDataSetChanged();
    }
}