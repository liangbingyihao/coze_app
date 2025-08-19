package sdk.chat.demo.robot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.gyf.immersionbar.ImmersionBar;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.utils.ToastHelper;

public class GuideActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private MaterialButton btnNext;
    private final int[] guideImages = {
            R.mipmap.ic_intro_1,
            R.mipmap.ic_intro_2,
            R.mipmap.ic_intro_3
    };
    private final String[] guideTitles = {
            "恩典留痕",
            "查经库藏",
            "每日恩语"
    };
    private final String[] guideDescriptions = {
            "语音/文字记录感动，\n时间轴串联成与神同行的恩典之路",
            "圣经背景、注释一站式收藏， \n真理不再碎片化",
            "个性化经文+实践指南\n助你活出信仰力量"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).init();
        setContentView(R.layout.activity_guide);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
//        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        // 设置适配器
        GuideViewAdapter adapter = new GuideViewAdapter(guideImages,guideTitles,guideDescriptions);
        viewPager.setAdapter(adapter);

        // 添加指示点
//        addDots(0);

        // 设置ViewPager页面改变监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
//                addDots(position);

                // 改变按钮文字
                if (position == guideImages.length - 1) {
                    btnNext.setText(getString(R.string.enter));
//                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText(getString(R.string.next));
//                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

//        btnSkip.setOnClickListener(v -> launchMainActivity());

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < guideImages.length - 1) {
                // 移动到下一页
                viewPager.setCurrentItem(current + 1);
            } else {
                launchMainActivity();
            }
        });
    }

//    private void addDots(int currentPosition) {
//        dotsLayout.removeAllViews();
//
//        for (int i = 0; i < guideImages.length; i++) {
//            View dot = new View(this);
//            dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_unselected));
//
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    dpToPx(8), dpToPx(8));
//            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
//            dot.setLayoutParams(params);
//
//            if (i == currentPosition) {
//                dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_selected));
//            }
//
//            dotsLayout.addView(dot);
//        }
//    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void launchMainActivity() {
        if (ChatSDK.currentUser()!=null) {
            startActivity(new Intent(this, MainDrawerActivity.class));
        }else{
            ToastHelper.show(this,R.string.network_error);
        }
        finish();
    }

    class GuideViewAdapter extends RecyclerView.Adapter<GuideViewAdapter.ViewHolder> {
        private int[] guideImages;
        private String[] guideTitles;
        private String[] guideDescriptions;

        public GuideViewAdapter(int[] images, String[] titles, String[] descriptions) {
            this.guideImages = images;
            this.guideTitles = titles;
            this.guideDescriptions = descriptions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageResource(guideImages[position]);
            holder.titleView.setText(guideTitles[position]);
            holder.descView.setText(guideDescriptions[position]);
        }

        @Override
        public int getItemCount() {
            return guideImages.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView titleView;
            TextView descView;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image);
                titleView = view.findViewById(R.id.title);
                descView = view.findViewById(R.id.description);
            }
        }
    }
}