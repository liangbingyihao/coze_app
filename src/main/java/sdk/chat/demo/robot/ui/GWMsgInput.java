package sdk.chat.demo.robot.ui;


import sdk.chat.demo.robot.extensions.ActivityExtensionsKt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.audio.AsrHelper;
import sdk.chat.demo.robot.extensions.LogHelper;
import sdk.chat.demo.robot.handlers.GWThreadHandler;

public class GWMsgInput extends RelativeLayout
        implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {

    public EditText messageInput;
    private EditText messagePrompt;
    private View inputContainer;
    private View buttonContainer;
    public View messageSendButton;
    private View stopSendButton;
    private ImageView attachmentButton;
    private ImageView editModeButton;
    //    public SoundWaveView soundWaveView;
    private CircleOverlayView circleOverlayView;
//    public Space sendButtonSpace, attachmentButtonSpace;

    private CharSequence input;
    private GWMsgInput.InputListener inputListener;
    private GWMsgInput.AttachmentsListener attachmentsListener;
    private boolean isTyping;
    private GWMsgInput.TypingListener typingListener;
    private int delayTypingStatusMillis;
    private long whenStartAsrMillis;
    private Integer lastLineCount = 0;
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

//    private static final int MSG_UPDATE_WAVE = 1;
//    private int soundWaveTimes;
//    private final Handler handler = new Handler(Looper.getMainLooper()) {
//        @Override
//        public void handleMessage(Message msg) {
//            if (msg.what == MSG_UPDATE_WAVE && soundWaveTimes > 0) {
//                --soundWaveTimes;
//                float amplitude = (float) (Math.random() * 0.8 + 0.2);
//                soundWaveView.updateAmplitude(amplitude);
//
//                if (soundWaveTimes > 0) {
//                    sendEmptyMessageDelayed(MSG_UPDATE_WAVE, 50);
//                } else {
//                    soundWaveView.reset();
//                }
//            }
//        }
//    };

//    public void startSimulation(int times) {
//        soundWaveTimes = times;
//        handler.removeMessages(MSG_UPDATE_WAVE);
//        handler.sendEmptyMessage(MSG_UPDATE_WAVE);
//    }
//
//    public void stopSimulation() {
//        soundWaveTimes = 0;
//        soundWaveView.reset();
//        handler.removeMessages(MSG_UPDATE_WAVE);
//    }
    public void onAsrStop(boolean error) {
        if (!error && System.currentTimeMillis() - whenStartAsrMillis < 700) {
            return;
        }
        startPos = -1;
        attachmentButton.setImageResource(R.mipmap.ic_audio);
        circleOverlayView.stopAnimation();
        circleOverlayView.setVisibility(View.GONE);
        messageInput.setShowSoftInputOnFocus(true);
        if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
//        stopSimulation();
    }

    public void onMsgStatusChanged(int status) {
        //status: 0: pending,1:idle
        Log.e("sending", "onMsgStatusChanged:" + status);
        if (status == 0) {
            messageSendButton.setVisibility(GONE);
            stopSendButton.setVisibility(VISIBLE);
        } else {
            messageSendButton.setVisibility(VISIBLE);
            stopSendButton.setVisibility(GONE);
        }

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.messageSendButton) {
            whenStartAsrMillis = 0;
            AsrHelper.INSTANCE.stopAsr();
            boolean isSubmitted = onSubmit();
            if (isSubmitted) {
//                LogHelper.INSTANCE.appendLog("send text success");
                messageInput.setText("");
                lastLength = 0;
                messagePrompt.setVisibility(GONE);
                setEditMode(0, false);
            } else {
                LogHelper.INSTANCE.appendLog("send text failed");
            }
            removeCallbacks(typingTimerRunnable);
            post(typingTimerRunnable);
        } else if (id == R.id.attachmentButton) {
            if (circleOverlayView.getVisibility() == View.VISIBLE) {
                whenStartAsrMillis = 0;
                AsrHelper.INSTANCE.stopAsr();
                messageInput.setShowSoftInputOnFocus(true);
            } else {
                attachmentButton.setImageResource(R.mipmap.ic_recording);
                circleOverlayView.setVisibility(View.VISIBLE);
                circleOverlayView.startAnimation();
                if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
                whenStartAsrMillis = System.currentTimeMillis();
                AsrHelper.INSTANCE.startAsr();
                messageInput.setShowSoftInputOnFocus(false);
            }
//            onAddAttachments();
//        } else if (id == R.id.stopAsr) {
//            whenStartAsrMillis = 0;
//            AsrHelper.INSTANCE.stopAsr();
//            messageInput.setShowSoftInputOnFocus(true);
////            attachmentButton.setImageResource(R.mipmap.ic_audio);
////            asrContainer.setVisibility(View.GONE);
////            if (attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
////            stopSimulation();
        } else if (id == R.id.messageStopButton) {
            Log.d("sending", "stop");
            ((GWThreadHandler) ChatSDK.thread()).stopPolling();
            onMsgStatusChanged(1);
//            view.setVisibility(INVISIBLE);
//            messageSendButton.setVisibility(VISIBLE);
        } else if (id == R.id.editMode) {
            setEditMode(editMode == 0 ? 1 : 0, true);
        }
    }

    /**
     * This method is called to notify you that, within s,
     * the count characters beginning at start have just replaced old text that had length before
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {
        input = s;
        messageSendButton.setEnabled(!s.toString().trim().isEmpty());
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
        if (messageInput.getLineCount() < lastLineCount || lastLineCount == 0) {
            if (typingListener != null) typingListener.onHeightChange();
        }
        startPos = -1;
        Log.d("AsrHelper1", "afterTextChanged and stop");
        AsrHelper.INSTANCE.stopAsr();
        checkExpand();
//        lastLineCount = messageInput.getLineCount();
//        Log.e("AsrHelper1", messageInput.getLineCount() + ",lastLineCount:" + lastLineCount.toString());
//        if (editMode == 0) {
//            if (lastLineCount < 3 && messagePrompt.getVisibility() != VISIBLE) {
//                editModeButton.setVisibility(GONE);
//            } else if (editModeButton.getVisibility() != VISIBLE) {
//                editModeButton.setVisibility(VISIBLE);
//            }
//        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (lastFocus && !hasFocus && typingListener != null) {
            typingListener.onStopTyping();
        }
        lastFocus = hasFocus;
    }

    private void checkExpand() {
        lastLineCount = messageInput.getLineCount();
        Log.e("AsrHelper1", messageInput.getLineCount() + ",lastLineCount:" + lastLineCount.toString());
        if (editMode == 0) {
            if (lastLineCount < 3 && messagePrompt.getVisibility() != VISIBLE) {
                editModeButton.setVisibility(GONE);
            } else if (editModeButton.getVisibility() != VISIBLE) {
                editModeButton.setVisibility(VISIBLE);
            }
        }
    }

    private boolean onSubmit() {
        return inputListener != null && inputListener.onSubmit(input);
    }

//    private void onAddAttachments() {
//        if (attachmentsListener != null) attachmentsListener.onAddAttachments();
//    }

    public String getMessagePrompt() {
        if (messagePrompt.getVisibility() == VISIBLE) {
            return messagePrompt.getText().toString();
        } else {
            return null;
        }
    }


    public void setMessagePrompt(String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            messagePrompt.setVisibility(VISIBLE);
            messagePrompt.setText(prompt);
            setEditMode(1, true);
        } else {
            messagePrompt.setVisibility(GONE);
        }
        if (typingListener != null) typingListener.onHeightChange();
    }

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
        messagePrompt = findViewById(R.id.messagePrompt);
        editModeButton = findViewById(R.id.editMode);
        inputContainer = findViewById(R.id.inputContainer);
        messageSendButton = findViewById(R.id.messageSendButton);
        stopSendButton = findViewById(R.id.messageStopButton);
        attachmentButton = findViewById(R.id.attachmentButton);
        buttonContainer = findViewById(R.id.buttonContainer);
//        soundWaveView = findViewById(R.id.soundWave);
        circleOverlayView = findViewById(R.id.circleOverlay);
//                circleOverlay.startAnimation()
//        attachmentButtonSpace = findViewById(R.id.attachmentButtonSpace);

        messageSendButton.setOnClickListener(this);
        stopSendButton.setOnClickListener(this);
        attachmentButton.setOnClickListener(this);
        editModeButton.setOnClickListener(this);
//        findViewById(R.id.stopAsr).setOnClickListener(this);
        messageInput.addTextChangedListener(this);
        messagePrompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    messagePrompt.setVisibility(View.GONE);
                    if (lastLineCount < 3 && editMode == 0) {
                        editModeButton.setVisibility(GONE);
                    }
                }

                if (typingListener != null && editMode == 0) typingListener.onHeightChange();
            }
        });
        messageInput.setText("");
        messageInput.setOnFocusChangeListener(this);


        messageInput.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < 1000) {
                        return false;
                    }
                    lastClickTime = currentTime;

                    // 处理点击抬起事件
//                    int cursorPosition = ((EditText) v).getOffsetForPosition(event.getX(), event.getY());
                    Log.d("AsrHelper1", "onTouch and stop");
                    startPos = -1;
                    AsrHelper.INSTANCE.stopAsr();

                    // 执行点击后的操作
//                    handleClickAtPosition(cursorPosition);

                    // 为了可访问性，调用performClick()
                    v.performClick();
                }
                return false; // 返回false允许事件继续传递
            }
        });


        postDelayed(setEditModeRunnable, 10);
//        setEditMode(0);

    }

    private int editMode = 0;//0:关闭，1：全屏

    private final Runnable setEditModeRunnable = new Runnable() {
        @Override
        public void run() {
            setEditMode();
        }
    };

    public void updateInputHeight() {
        //键盘变更时
        if (editMode == 1) {
            setEditMode(1, false);
        }
    }

    public boolean isFullScreen() {
        return editMode == 1;
    }

    public String getDraft() {
        String prompt = messagePrompt.getVisibility() == View.VISIBLE ? messagePrompt.getText().toString() : "";
        String draft = messageInput.getText().toString();
        if (prompt.isEmpty() && draft.isEmpty()) {
            return null;
        }
        JsonObject o = new JsonObject();
        if (!prompt.isEmpty()) {
            o.addProperty("prompt", prompt);
        }
        if (!draft.isEmpty()) {
            o.addProperty("draft", draft);
        }
        return o.toString();
    }

    public void setDraft(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        if (jsonObject != null) {
            String prompt = jsonObject.has("prompt") ? jsonObject.get("prompt").getAsString() : "";
            String draft = jsonObject.has("draft") ? jsonObject.get("draft").getAsString() : "";
            if (draft != null && !draft.isEmpty()) {
                messageInput.setText(draft);
                lastLength = messageInput.getLineCount();
            }

            if (prompt != null && !prompt.isEmpty()) {
                messagePrompt.setVisibility(VISIBLE);
                messagePrompt.setText(prompt);
            }

            setEditMode(0, false);
        }
    }

    private void setEditMode(int editMode, boolean showKeyBoard) {
        removeCallbacks(setEditModeRunnable);
        this.editMode = editMode;
        postDelayed(setEditModeRunnable, 100);
        if (showKeyBoard && attachmentsListener != null) attachmentsListener.onChangeKeyboard(true);
    }

    private FrameLayout.LayoutParams paramsContract;
    private FrameLayout.LayoutParams paramsExpand;

    private void setEditMode() {
        // 获取并转换布局参数
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) inputContainer.getLayoutParams();

        // 设置边距
        if (editMode == 0) {
            if (paramsContract == null) {
                paramsContract = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                paramsContract.leftMargin = ActivityExtensionsKt.dpToPx(58, this.getContext());
                paramsContract.rightMargin = (int) buttonContainer.getWidth() + ActivityExtensionsKt.dpToPx(20, this.getContext());
                int m = ActivityExtensionsKt.dpToPx(4, this.getContext());
                paramsContract.topMargin = m;
                paramsContract.bottomMargin = m;
                paramsContract.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                Log.e("setEditMode", String.format("paramsContract:%d,%d,%d,%d",
                        paramsContract.leftMargin, paramsContract.rightMargin, paramsContract.topMargin, paramsContract.bottomMargin));
            }
            Log.e("setEditMode", String.format("paramsContract 1:%d,%d,%d,%d",
                    paramsContract.leftMargin, paramsContract.rightMargin, paramsContract.topMargin, paramsContract.bottomMargin));
            params.leftMargin = paramsContract.leftMargin;
            params.rightMargin = paramsContract.rightMargin;
            params.topMargin = paramsContract.topMargin;
            params.bottomMargin = paramsContract.bottomMargin;
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            messageInput.setMaxLines(6);
            editModeButton.setImageResource(R.mipmap.ic_expand);
            if (messagePrompt.getVisibility() == VISIBLE) {
                editModeButton.setVisibility(VISIBLE);
            } else {
                lastLineCount = messageInput.getLineCount();
                if (lastLineCount < 3) {
                    editModeButton.setVisibility(GONE);
                } else {
                    editModeButton.setVisibility(VISIBLE);
                }
            }
            inputContainer.setBackgroundResource(R.drawable.edittext_rounded_bg);
        } else {
            editModeButton.setVisibility(VISIBLE);
            if (paramsExpand == null) {
                int m = ActivityExtensionsKt.dpToPx(24, this.getContext());
                paramsExpand = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                paramsExpand.leftMargin = m;
                paramsExpand.rightMargin = m;
                paramsExpand.topMargin = m;
                paramsExpand.bottomMargin = m + messageInput.getHeight();
                paramsExpand.height = getResources().getDisplayMetrics().heightPixels - m - paramsExpand.bottomMargin;
                Log.e("setEditMode", String.format("paramsExpand:%d,%d,%d,%d",
                        paramsExpand.leftMargin, paramsExpand.rightMargin, paramsExpand.topMargin, paramsExpand.bottomMargin));
            }
            Log.e("setEditMode", String.format("paramsExpand 1:%d,%d,%d,%d",
                    paramsExpand.leftMargin, paramsExpand.rightMargin, paramsExpand.topMargin, paramsExpand.bottomMargin));
            int h = paramsExpand.height - typingListener.getKeyboardHeight();
            if (h == params.height) {
//                Log.e("setEditMode", "no change..heightPixels:" + getResources().getDisplayMetrics().heightPixels + ",getKeyboardHeight:" + typingListener.getKeyboardHeight());
                return;
            }
//            Log.e("setEditMode", "change..heightPixels:" + getResources().getDisplayMetrics().heightPixels + ",getKeyboardHeight:" + typingListener.getKeyboardHeight() + ",h:" + h);
            params.height = h;
            params.leftMargin = paramsExpand.leftMargin;
            params.rightMargin = paramsExpand.rightMargin;
            params.topMargin = paramsExpand.topMargin;
            params.bottomMargin = messageSendButton.getHeight();
            messageInput.setMaxLines(Integer.MAX_VALUE);
            editModeButton.setImageResource(R.mipmap.ic_contract);
            //FIXME
//            inputContainer.setBackgroundResource(R.drawable.edittext_rounded_white_bg);
        }
// 应用新的布局参数
        inputContainer.setLayoutParams(params);

// 请求重新布局
        inputContainer.requestLayout();

        if (typingListener != null) {
            typingListener.onHeightChange();
        }
    }

    private int startPos = -1;
    private int lastLength = 0;

    public void smartInsertAtCursor(Boolean definite, String newText) {
        if (newText == null || newText.isEmpty()) {
            return;
        }

        try {
            Editable editable = messageInput.getText();

            if (startPos < 0) {
                startPos = messageInput.getSelectionStart();

                // 处理没有光标位置的情况
                if (startPos < 0) {
                    startPos = editable.length();
                }
                lastLength = 0;
                LogHelper.INSTANCE.appendLog("set startPos: " + startPos + ", for " + newText);
            }
            Log.e("AsrHelper1", "definite: " + definite + ",newText:" + newText + ",startPos:" + startPos);
//            editable.insert(startPos, newText);
            messageInput.removeTextChangedListener(this);

//            AsrHelper.INSTANCE.appendLog("1:startPos:" + startPos + "," + lastLength + "," + editable.toString().substring(startPos, startPos + lastLength) + " TO " + newText);
            editable.replace(startPos, startPos + lastLength, newText);
            messageSendButton.setEnabled(input.length() > 0);
            checkExpand();
//            AsrHelper.INSTANCE.appendLog("2:" + editable.toString());
            lastLength = newText.length();
//            messageInput.setSelection(startPos + lastLength);
            if (definite) {
                startPos += newText.length();
                lastLength = 0;
            }
            messageInput.addTextChangedListener(this);
        } catch (Exception e) {
            LogHelper.INSTANCE.appendLog("smartInsertAtCursor:" + e.toString());
        }
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

        void onHeightChange();

        int getKeyboardHeight();

    }
}
