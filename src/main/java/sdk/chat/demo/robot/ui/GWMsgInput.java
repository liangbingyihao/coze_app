package sdk.chat.demo.robot.ui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import java.lang.reflect.Field;

import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.audio.AsrHelper;

public class GWMsgInput extends RelativeLayout
        implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {

    public EditText messageInput;
    public View asrContainer;
    public View messageSendButton;
    public ImageView attachmentButton;
    public SoundWaveView soundWaveView;
//    public Space sendButtonSpace, attachmentButtonSpace;

    private CharSequence input;
    private GWMsgInput.InputListener inputListener;
    private GWMsgInput.AttachmentsListener attachmentsListener;
    private boolean isTyping;
    private GWMsgInput.TypingListener typingListener;
    private int delayTypingStatusMillis;
    private long whenStartAsrMillis;
    private final Runnable typingTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTyping) {
                isTyping = false;
                if (typingListener != null) typingListener.onStopTyping();
            }
        }
    };
    private boolean lastFocus;

    public GWMsgInput(Context context) {
        super(context);
        init(context);
    }

    public GWMsgInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GWMsgInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Sets callback for 'submit' button.
     *
     * @param inputListener input callback
     */
    public void setInputListener(GWMsgInput.InputListener inputListener) {
        this.inputListener = inputListener;
    }

    /**
     * Sets callback for 'add' button.
     *
     * @param attachmentsListener input callback
     */
    public void setAttachmentsListener(GWMsgInput.AttachmentsListener attachmentsListener) {
        this.attachmentsListener = attachmentsListener;
    }

    /**
     * Returns EditText for messages input
     *
     * @return EditText
     */
    public EditText getInputEditText() {
        return messageInput;
    }

    /**
     * Returns `submit` button
     *
     * @return ImageButton
     */
//    public ImageButton getButton() {
//        return messageSendButton;
//    }

    private static final int MSG_UPDATE_WAVE = 1;
    private int soundWaveTimes;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_WAVE && soundWaveTimes > 0) {
                --soundWaveTimes;
                float amplitude = (float) (Math.random() * 0.8 + 0.2);
                soundWaveView.updateAmplitude(amplitude);

                if (soundWaveTimes > 0) {
                    sendEmptyMessageDelayed(MSG_UPDATE_WAVE, 50);
                } else {
                    soundWaveView.reset();
                }
            }
        }
    };

    public void startSimulation(int times) {
        soundWaveTimes = times;
        handler.removeMessages(MSG_UPDATE_WAVE);
        handler.sendEmptyMessage(MSG_UPDATE_WAVE);
    }

    public void stopSimulation() {
        soundWaveTimes = 0;
        soundWaveView.reset();
        handler.removeMessages(MSG_UPDATE_WAVE);
    }

    public void onAsrStop() {
        if (System.currentTimeMillis() - whenStartAsrMillis < 700) {
            return;
        }
        attachmentButton.setImageResource(R.mipmap.ic_audio);
        asrContainer.setVisibility(View.GONE);
        if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
        stopSimulation();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.messageSendButton) {
            boolean isSubmitted = onSubmit();
            if (isSubmitted) {
                messageInput.setText("");
            }
            removeCallbacks(typingTimerRunnable);
            post(typingTimerRunnable);
        } else if (id == R.id.attachmentButton) {
            if (asrContainer.getVisibility() == View.VISIBLE) {
                whenStartAsrMillis = 0;
                AsrHelper.INSTANCE.stopAsr();
            } else {
                attachmentButton.setImageResource(R.mipmap.ic_show_kb);
                asrContainer.setVisibility(View.VISIBLE);
                if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(false);
                whenStartAsrMillis = System.currentTimeMillis();
                AsrHelper.INSTANCE.startAsr();
            }
//            onAddAttachments();
        } else if (id == R.id.stopAsr) {
            whenStartAsrMillis = 0;
            AsrHelper.INSTANCE.stopAsr();
//            attachmentButton.setImageResource(R.mipmap.ic_audio);
//            asrContainer.setVisibility(View.GONE);
//            if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
//            stopSimulation();
        }
    }

    /**
     * This method is called to notify you that, within s,
     * the count characters beginning at start have just replaced old text that had length before
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {
        input = s;
        messageSendButton.setEnabled(input.length() > 0);
        if (s.length() > 0) {
            if (!isTyping) {
                isTyping = true;
                if (typingListener != null) typingListener.onStartTyping();
            }
            removeCallbacks(typingTimerRunnable);
            postDelayed(typingTimerRunnable, delayTypingStatusMillis);
        }
    }

    /**
     * This method is called to notify you that, within s,
     * the count characters beginning at start are about to be replaced by new text with length after.
     */
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        //do nothing
    }

    /**
     * This method is called to notify you that, somewhere within s, the text has been changed.
     */
    @Override
    public void afterTextChanged(Editable editable) {
        //do nothing
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (lastFocus && !hasFocus && typingListener != null) {
            typingListener.onStopTyping();
        }
        lastFocus = hasFocus;
    }

    private boolean onSubmit() {
        return inputListener != null && inputListener.onSubmit(input);
    }

//    private void onAddAttachments() {
//        if (attachmentsListener != null) attachmentsListener.onAddAttachments();
//    }

    public void init(Context context, AttributeSet attrs) {
        init(context);
//
//        setPadding(20,10,20,30);
//        setBackgroundResource(R.drawable.top_rounded_corners);
    }


    public void init(Context context) {
        // Causes some dropped frames... but it's the system
        LayoutInflater.from(context).inflate(R.layout.view_message_input, this);
//        inflate(context, R.layout.view_message_input, this);

        messageInput = findViewById(R.id.messageInput);
        messageSendButton = findViewById(R.id.messageSendButton);
        attachmentButton = findViewById(R.id.attachmentButton);
        asrContainer = findViewById(R.id.asrContainer);
        soundWaveView = findViewById(R.id.soundWave);
//        attachmentButtonSpace = findViewById(R.id.attachmentButtonSpace);

        messageSendButton.setOnClickListener(this);
        attachmentButton.setOnClickListener(this);
        findViewById(R.id.stopAsr).setOnClickListener(this);
        messageInput.addTextChangedListener(this);
        messageInput.setText("");
        messageInput.setOnFocusChangeListener(this);
    }

    private void setCursor(Drawable drawable) {
        if (drawable == null) return;

        try {
            @SuppressLint("SoonBlockedPrivateApi") final Field drawableResField = TextView.class.getDeclaredField("mCursorDrawableRes");
            drawableResField.setAccessible(true);

            final Object drawableFieldOwner;
            final Class<?> drawableFieldClass;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                drawableFieldOwner = this.messageInput;
                drawableFieldClass = TextView.class;
            } else {
                final Field editorField = TextView.class.getDeclaredField("mEditor");
                editorField.setAccessible(true);
                drawableFieldOwner = editorField.get(this.messageInput);
                drawableFieldClass = drawableFieldOwner.getClass();
            }
            final Field drawableField = drawableFieldClass.getDeclaredField("mCursorDrawable");
            drawableField.setAccessible(true);
            drawableField.set(drawableFieldOwner, new Drawable[]{drawable, drawable});
        } catch (Exception ignored) {
        }
    }

    public void setTypingListener(GWMsgInput.TypingListener typingListener) {
        this.typingListener = typingListener;
    }

    /**
     * Interface definition for a callback to be invoked when user pressed 'submit' button
     */
    public interface InputListener {

        /**
         * Fires when user presses 'send' button.
         *
         * @param input input entered by user
         * @return if input text is valid, you must return {@code true} and input will be cleared, otherwise return false.
         */
        boolean onSubmit(CharSequence input);
    }

    /**
     * Interface definition for a callback to be invoked when user presses 'add' button
     */
    public interface AttachmentsListener {

        /**
         * Fires when user presses 'add' button.
         */
        void onChangeKeyboard(boolean show);
    }

    /**
     * Interface definition for a callback to be invoked when user typing
     */
    public interface TypingListener {

        /**
         * Fires when user presses start typing
         */
        void onStartTyping();

        /**
         * Fires when user presses stop typing
         */
        void onStopTyping();

    }
}
