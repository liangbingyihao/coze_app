package sdk.chat.demo.robot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.gyf.immersionbar.ImmersionBar;

import java.util.Locale;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.extensions.LanguageUtils;
import sdk.chat.ui.utils.ToastHelper;

public class GuideActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private MaterialButton btnNext;
    private final int[] guideImages = {
            R.mipmap.ic_intro_m1,
            R.mipmap.ic_intro_m2,
            R.mipmap.ic_intro_m3
    };
    private final int[] guideTitles = {
            R.string.guide_1,
            R.string.guide_2,
            R.string.guide_3,
    };
    private final int[] guideDescriptions = {
            R.string.guide_desc_1,
            R.string.guide_desc_2,
            R.string.guide_desc_3,
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

        var lang = Locale.getDefault().toLanguageTag();
        int h = dpToPx(300);
        if (lang.contains("en")) {
            int[] hs = {h, h * 2, h * 2};
            GuideViewAdapter adapter = new GuideViewAdapter(guideImages, guideTitles, guideDescriptions, hs);
            viewPager.setAdapter(adapter);
        } else {
            int[] hs = {h, h, h};
            GuideViewAdapter adapter = new GuideViewAdapter(guideImages, guideTitles, guideDescriptions, hs);
            viewPager.setAdapter(adapter);
        }

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
        if (ChatSDK.currentUser() != null) {
            startActivity(new Intent(this, MainDrawerActivity.class));
            finish();
        } else {
            ToastHelper.show(this, R.string.network_error);
        }
    }

    class GuideViewAdapter extends RecyclerView.Adapter<GuideViewAdapter.ViewHolder> {
        private int[] guideImages;
        private int[] guideTitles;
        private int[] guideDescriptions;
        private int[] maskHeights;

        public GuideViewAdapter(int[] images, int[] titles, int[] descriptions, int[] maskHeights) {
            this.guideImages = images;
            this.guideTitles = titles;
            this.guideDescriptions = descriptions;
            this.maskHeights = maskHeights;
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
            holder.titleView.setText(getString(guideTitles[position]));
            holder.descView.setText(getString(guideDescriptions[position]));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.maskView.getLayoutParams();
            params.height = maskHeights[position]; // 设置新高度（单位：像素）
            holder.maskView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return guideImages.length;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView titleView;
            TextView descView;
            View maskView;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image);
                titleView = view.findViewById(R.id.title);
                descView = view.findViewById(R.id.description);
                maskView = view.findViewById(R.id.image_mask);
            }
        }
    }
}