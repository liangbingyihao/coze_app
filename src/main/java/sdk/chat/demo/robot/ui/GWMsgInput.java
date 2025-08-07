package sdk.chat.demo.robot.ui;


import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Space;

public class GWMsgInput extends RelativeLayout
        implements View.OnClickListener, TextWatcher, View.OnFocusChangeListener {

    public EditText messageInput;
    public ImageButton messageSendButton;
    public ImageButton attachmentButton;
    public Space sendButtonSpace, attachmentButtonSpace;

    private CharSequence input;
    private GWMsgInput.InputListener inputListener;
    private GWMsgInput.AttachmentsListener attachmentsListener;
    private boolean isTyping;
    private GWMsgInput.TypingListener typingListener;
    private int delayTypingStatusMillis;
    private Runnable typingTimerRunnable = new Runnable() {
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
    public ImageButton getButton() {
        return messageSendButton;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == com.stfalcon.chatkit.R.id.messageSendButton) {
            boolean isSubmitted = onSubmit();
            if (isSubmitted) {
                messageInput.setText("");
            }
            removeCallbacks(typingTimerRunnable);
            post(typingTimerRunnable);
        } else if (id == com.stfalcon.chatkit.R.id.attachmentButton) {
            onAddAttachments();
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

    private void onAddAttachments() {
        if (attachmentsListener != null) attachmentsListener.onAddAttachments();
    }

    public void init(Context context, AttributeSet attrs) {
        init(context);
    }


    public void init(Context context) {
        // Causes some dropped frames... but it's the system
        LayoutInflater.from(context).inflate(com.stfalcon.chatkit.R.layout.view_message_input, this);
//        inflate(context, R.layout.view_message_input, this);

        messageInput = findViewById(com.stfalcon.chatkit.R.id.messageInput);
        messageSendButton = findViewById(com.stfalcon.chatkit.R.id.messageSendButton);
        attachmentButton = findViewById(com.stfalcon.chatkit.R.id.attachmentButton);
        sendButtonSpace = findViewById(com.stfalcon.chatkit.R.id.sendButtonSpace);
        attachmentButtonSpace = findViewById(com.stfalcon.chatkit.R.id.attachmentButtonSpace);

        messageSendButton.setOnClickListener(this);
        attachmentButton.setOnClickListener(this);
        messageInput.addTextChangedListener(this);
        messageInput.setText("");
        messageInput.setOnFocusChangeListener(this);
    }

//    private void setCursor(Drawable drawable) {
//        if (drawable == null) return;
//
//        try {
//            final Field drawableResField = TextView.class.getDeclaredField("mCursorDrawableRes");
//            drawableResField.setAccessible(true);
//
//            final Object drawableFieldOwner;
//            final Class<?> drawableFieldClass;
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
//                drawableFieldOwner = this.messageInput;
//                drawableFieldClass = TextView.class;
//            } else {
//                final Field editorField = TextView.class.getDeclaredField("mEditor");
//                editorField.setAccessible(true);
//                drawableFieldOwner = editorField.get(this.messageInput);
//                drawableFieldClass = drawableFieldOwner.getClass();
//            }
//            final Field drawableField = drawableFieldClass.getDeclaredField("mCursorDrawable");
//            drawableField.setAccessible(true);
//            drawableField.set(drawableFieldOwner, new Drawable[]{drawable, drawable});
//        } catch (Exception ignored) {
//        }
//    }

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
        void onAddAttachments();
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
