package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;

import de.hdodenhof.circleimageview.CircleImageView;
import sdk.chat.core.dao.Thread;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.appbar.ChatActionBar;
import sdk.chat.ui.module.UIModule;

public class CozeChatActionBar extends ChatActionBar {

    protected OnClickListener onClickListener;
    public TextView titleTextView;
    public CircleImageView imageView;
    public TextView subtitleTextView;
    public ImageView searchImageView;
    public Toolbar toolbar;
    public AppBarLayout appBarLayout;


    public CozeChatActionBar(Context context) {
        super(context);
        initViews();
    }

    public CozeChatActionBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public CozeChatActionBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    public void initViews() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_coze_bar_chat, this);

        titleTextView = findViewById(R.id.titleTextView);
//        imageView = findViewById(R.id.imageView);
        subtitleTextView = findViewById(R.id.subtitleTextView);
        searchImageView = findViewById(R.id.searchImageView);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);


        titleTextView.setOnClickListener(this::onClick);
//        imageView.setOnClickListener(this::onClick);
        subtitleTextView.setOnClickListener(this::onClick);
        searchImageView.setImageDrawable(ChatSDKUI.icons().get(getContext(), ChatSDKUI.icons().search, ChatSDKUI.icons().actionBarIconColor));
    }

    public void onClick(View view) {
        if (UIModule.config().threadDetailsEnabled && onClickListener != null) {
            onClickListener.onClick(view);
        }
    }

    public void reload(Thread thread) {
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setTypingText(Thread thread, final String text) {
        setSubtitleText(thread, text);
    }

    public void setSubtitleText(Thread thread, final String text) {
        subtitleTextView.setText(text);
    }

    public String getDefaultText() {
        return getContext().getString(R.string.tap_here_for_contact_info);
    }

    public void hideText() {
        titleTextView.setVisibility(View.GONE);
        subtitleTextView.setVisibility(View.GONE);
    }

    public void showText() {
        titleTextView.setVisibility(View.VISIBLE);
        subtitleTextView.setVisibility(View.VISIBLE);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void onSearchClicked(OnClickListener listener) {
        searchImageView.setOnClickListener(listener);
    }

    public void hideSearchIcon() {
        searchImageView.setVisibility(View.INVISIBLE);
    }

    public void showSearchIcon() {
        if (UIModule.config().messageSearchEnabled) {
            searchImageView.setVisibility(View.VISIBLE);
        }
    }
}
