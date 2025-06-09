package sdk.chat.demo.robot.adpter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import sdk.chat.demo.pre.R;

public class FooterAdapter extends RecyclerView.Adapter<FooterAdapter.ViewHolder> {
    static class ViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        ViewHolder(View view) {
            super(view);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_footer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Footer 不需要绑定数据
    }

    @Override
    public int getItemCount() {
        return 1; // 只有一个 Footer
    }
}
