package sdk.chat.demo.robot.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentContainerView;

import com.lassi.common.utils.KeyUtils;
import com.stfalcon.chatkit.messages.MessageInput;

import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.MessageMetaValue;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.handlers.TypingIndicatorHandler;
import sdk.chat.core.interfaces.ChatOption;
import sdk.chat.core.interfaces.ChatOptionsDelegate;
import sdk.chat.core.interfaces.ChatOptionsHandler;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageType;
import sdk.chat.core.ui.AbstractKeyboardOverlayFragment;
import sdk.chat.core.ui.KeyboardOverlayHandler;
import sdk.chat.core.ui.Sendable;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.core.utils.StringChecker;
import sdk.chat.demo.examples.helper.CustomPrivateThreadsFragment;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.audio.AsrHelper;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.ui.GWChatContainer;
import sdk.chat.demo.robot.ui.GWMsgInput;
import sdk.chat.demo.robot.ui.KeyboardOverlayHelper;
import sdk.chat.demo.robot.ui.listener.GWClickListener;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.ui.activities.preview.ChatPreviewActivity;
import sdk.chat.demo.robot.ui.AudioBinder;
import sdk.chat.ui.fragments.AbstractChatFragment;
import sdk.chat.ui.interfaces.TextInputDelegate;
import sdk.chat.ui.keyboard.KeyboardAwareFrameLayout;
import sdk.chat.ui.module.UIModule;
import sdk.chat.ui.utils.ToastHelper;
import sdk.chat.ui.views.ReplyView;
import sdk.guru.common.DisposableMap;
import sdk.guru.common.RX;

public class GWChatFragment extends AbstractChatFragment implements GWChatContainer.Delegate, TextInputDelegate, ChatOptionsDelegate, KeyboardOverlayHandler {

    protected View rootView;

    protected Thread thread;
    protected ChatOptionsHandler optionsHandler;
    // Should we remove the user from the public chat when we stop this activity?
    // If we are showing a temporary screen like the sticker text screen
    // this should be set to no
    protected boolean removeUserFromChatOnExit = true;
    protected static boolean enableTrace = false;
    protected GWChatContainer chatView;
    //    protected View divider;
    protected View replyView;
    protected TextView replyText;
    protected GWMsgInput input;
    protected CoordinatorLayout listContainer;
    protected KeyboardAwareFrameLayout root;
    protected LinearLayout messageInputLinearLayout;
    protected FragmentContainerView keyboardOverlay;

    protected ActivityResultLauncher<Intent> launcher;

    //    protected AudioBinder audioBinder = null;
    protected DisposableMap dm = new DisposableMap();

    protected KeyboardOverlayHelper koh;

    private String messageId;

    public interface DataCallback {
        Long getMessageId();
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_gw_chat;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void reloadData() {
        chatView.notifyDataSetChanged();
    }

    @Override
    public void onClick(Message message) {
        if (getActivity() != null) {
            if (message.getMessageType().is(MessageType.System)) {
                for (MessageMetaValue v : message.getMetaValues()) {
                    if ("action".equals(v.getKey()) && "1".equals(v.getValue())) {
                        handleMessageSend(ChatSDK.thread().sendMessageWithText(message.getText(), thread));
                        return;
                    }
                }
            }
            ChatSDKUI.shared().getMessageRegistrationManager().onClick(getActivity(), root, message);
        }
    }

    @Override
    public void onLongClick(Message message) {
        if (getActivity() != null) {
            ChatSDKUI.shared().getMessageRegistrationManager().onLongClick(getActivity(), root, message);
        }
    }

    @Override
    public String getMessageId() {
        return messageId;
//        return 0L;
    }


    public void setThread(Thread thread) {
        if (true) {
            return;
        }
        this.thread = thread;
        Bundle bundle = new Bundle();
        bundle.putString(Keys.IntentKeyThreadEntityID, thread.getEntityID());
        setArguments(bundle);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取参数
        Bundle args = getArguments();
        if (args != null) {
            messageId = args.getString("KEY_MESSAGE_ID", null);
        }
    }

    @Override
    public View onCreateView(@io.reactivex.annotations.NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = super.onCreateView(inflater, container, savedInstanceState);
        restoreState(savedInstanceState);

        initViews();


        koh = new KeyboardOverlayHelper(this);
        koh.showOptionsKeyboardOverlay();

        input.getInputEditText().setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null && getActivity() != null) {
                    Serializable media = data.getSerializableExtra(KeyUtils.SELECTED_MEDIA);
                    if (media != null) {
                        // Pass these extras to the chat preview activity
                        Intent intent = new Intent(getActivity(), ChatPreviewActivity.class);
                        intent.putExtra(KeyUtils.SELECTED_MEDIA, media);
                        intent.putExtra(Keys.IntentKeyThreadEntityID, thread.getEntityID());
                        getActivity().startActivity(intent);
                    }
                }
            }
        });


        return rootView;
    }

    public View inflate(@io.reactivex.annotations.NonNull LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(getLayout(), container, false);
    }

//    public void setChatViewBottomMargin(int margin) {
//        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) chatView.getLayoutParams();
//        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, margin);
//        chatView.setLayoutParams(params);
//    }

//    public void updateOptionsButton() {
//        input.findViewById(sdk.chat.ui.R.id.attachmentButton).setVisibility(chatView.getSelectedMessages().isEmpty() ? View.VISIBLE : View.GONE);
//        input.findViewById(sdk.chat.ui.R.id.attachmentButtonSpace).setVisibility(chatView.getSelectedMessages().isEmpty() ? View.VISIBLE : View.GONE);
//    }

    public void hideTextInput() {
        input.setVisibility(View.GONE);
//        divider.setVisibility(View.GONE);
        updateChatViewMargins(true);
    }

    public void showTextInput() {
        input.setVisibility(View.VISIBLE);
//        divider.setVisibility(View.VISIBLE);
        updateChatViewMargins(true);
    }

    public void hideReplyView() {
        replyView.setVisibility(View.GONE);
        updateChatViewMargins(true);
    }

    public void showReplyView() {
        replyView.setVisibility(View.VISIBLE);
        updateChatViewMargins(true);
    }


    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable marginUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            int bottomMargin = bottomMargin();

            if (koh.keyboardOverlayVisible()) {
                bottomMargin += getKeyboardAwareView().getKeyboardHeight();
            }

            CoordinatorLayout.LayoutParams params =
                    (CoordinatorLayout.LayoutParams) chatView.getLayoutParams();
            params.setMargins(
                    params.leftMargin,
                    params.topMargin,
                    params.rightMargin,
                    bottomMargin
            );
            chatView.setLayoutParams(params);
        }
    };


    public void updateChatViewMargins(boolean post) {
//        Runnable runnable = () -> {
//            int bottomMargin = bottomMargin();
//
//            if (koh.keyboardOverlayVisible()) {
//                bottomMargin += getKeyboardAwareView().getKeyboardHeight();
//            }
//
//            // TODO: Margins
//            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) chatView.getLayoutParams();
//            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin);
//            chatView.setLayoutParams(params);
//        };
//
//        if (post) {
//            input.post(runnable);
//        } else {
//            runnable.run();
//        }

        // 移除之前未执行的更新
        handler.removeCallbacks(marginUpdateRunnable);

        if (post) {
            // 延迟100ms执行，确保短时间内多次调用只执行最后一次
            handler.postDelayed(marginUpdateRunnable, 100);
        } else {
            marginUpdateRunnable.run();
        }

    }

    public int bottomMargin() {
        int bottomMargin = 0;
        if (replyView.getVisibility() == View.VISIBLE) {
            bottomMargin += replyView.getHeight();
        }
        if (input.getVisibility() != View.GONE) {
            bottomMargin += input.getHeight();
        }
        return bottomMargin;
    }

//    public void showReplyView(String title, String imageURL, String text) {
//        koh.hideKeyboardOverlayAndShowKeyboard();
//        updateOptionsButton();
//        if (audioBinder != null) {
//            audioBinder.showReplyView();
//        }
//        replyView.show(title, imageURL, text);
//
//        updateChatViewMargins(true);
//    }

    protected void initViews() {
        chatView = rootView.findViewById(sdk.chat.ui.R.id.chatView);
        replyText = rootView.findViewById(R.id.tvReply);
        replyView = rootView.findViewById(sdk.chat.ui.R.id.replyView);
        input = rootView.findViewById(sdk.chat.ui.R.id.input);
        listContainer = rootView.findViewById(sdk.chat.ui.R.id.listContainer);
        root = rootView.findViewById(sdk.chat.ui.R.id.root);
        messageInputLinearLayout = rootView.findViewById(sdk.chat.ui.R.id.messageInputLinearLayout);
        keyboardOverlay = rootView.findViewById(sdk.chat.ui.R.id.keyboardOverlay);

        chatView.setDelegate(this);
        chatView.initViews();


        View.OnTouchListener keyboardDismissTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("GW_ACTION_DOWN", "GW_ACTION_DOWN");
                    View currentFocus = getActivity().getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        Rect rect = new Rect();
                        currentFocus.getGlobalVisibleRect(rect);
                        if (!rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            currentFocus.clearFocus();
                            hideKeyboard();
                        }
                    }
                }
                return false;
            }
        };
        chatView.findViewById(R.id.recyclerview).setOnTouchListener(keyboardDismissTouchListener);
//        chatView.findViewById(R.id.root).setOnTouchListener(keyboardDismissTouchListener);


        GWClickListener.registerListener((BaseActivity) getActivity(), chatView.getMessagesListAdapter());
//
//        if (UIModule.config().messageSelectionEnabled) {
//            chatView.enableSelectionMode(count -> {
//                updateOptionsButton();
//            });
//        }

//        if (!hasVoice(ChatSDK.currentUser())) {
//            hideTextInput();
//        }

        input.setInputListener(input -> {
            sendMessage(String.valueOf(input));
            return true;
        });


        addTypingListener();

        input.setAttachmentsListener(this::toggleOptions);

//        replyView.setOnCancelListener(v -> hideReplyView());

        setChatState(TypingIndicatorHandler.State.active);

        rootView.findViewById(R.id.closeReply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideReplyView();
            }
        });
        ;

        if (enableTrace) {
            Debug.startMethodTracing("chat");
        }

        addListeners();

    }

    protected void addListeners() {
//        dm.add(ChatSDK.events().sourceOnMain()
//                .filter(NetworkEvent.filterType(EventType.ThreadMetaUpdated, EventType.ThreadUserAdded, EventType.ThreadUserRemoved))
//                .filter(NetworkEvent.filterThreadEntityID(thread.getEntityID()))
//                .subscribe(networkEvent -> {
//                    // If we are added, we will get voice...
//                    User user = networkEvent.getUser();
//                    if (user != null && user.isMe()) {
//                        showOrHideTextInputView();
//                    }
//                }));
//
//        dm.add(ChatSDK.events().sourceOnMain()
//                .filter(NetworkEvent.filterType(EventType.UserMetaUpdated, EventType.UserPresenceUpdated))
//                .filter(networkEvent -> thread.containsUser(networkEvent.getUser()))
//                .subscribe(networkEvent -> {
//                    reloadData();
//                }));

        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.TypingStateUpdated))
                .filter(NetworkEvent.filterThreadEntityID(thread.getEntityID()))
                .subscribe(networkEvent -> {
                    String typingText = networkEvent.getText();
                    if (typingText != null) {
                        typingText += getString(sdk.chat.ui.R.string.typing);
                    }
                    Logger.debug(typingText);
                }));


        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageAdded))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkEvent -> {
                    input.onMsgStatusChanged(0);
                    hideKeyboard();
                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageUpdated, EventType.MessageSendStatusUpdated))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkEvent -> {
                    GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
                    if (handler.pendingMsgId() == null) {
                        input.onMsgStatusChanged(1);
                    } else {
                        input.onMsgStatusChanged(0);
                    }
                }));

        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.MessageInputPrompt))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkEvent -> {
//                    showTextInput();
                    EditText editText = input.getInputEditText();
                    editText.requestFocus();
                    replyText.setText(networkEvent.getText());
                    showReplyView();
                    Map<String, Object> params = networkEvent.getData();
                    Object placeHolder = params.get("default");
                    if(placeHolder!=null){
                        editText.setText((CharSequence) placeHolder);
                    }

                    editText.postDelayed(() -> {
                        editText.setSelection(editText.getText().length());
                        showKeyboard();
                    }, 500);
                }));

        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.MessageInputAsr))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkEvent -> {
                    if (networkEvent.getIsOnline()) {
                        showTextInput();
                        EditText editText = input.getInputEditText();
                        Map<String, Object> params = networkEvent.getData();
                        input.smartInsertAtCursor((Boolean) params.get("definite"), (String) params.get("newMsg"));
                        input.startSimulation(50);
                    } else {
                        Log.e("AsrHelper", "onAsrStop");
                        input.onAsrStop(false);
                    }
                }));
        dm.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.Error))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkEvent -> {
                    Map<String, Object> params = networkEvent.getData();
                    String errType = (String) params.getOrDefault("type", "");
                    String msg = (String) params.getOrDefault("msg", "error...");
                    if ("asr".equals(errType)) {
                        ToastHelper.show(getActivity(), msg);
                        input.onAsrStop(true);
                        if ("ERR_REC_CHECK_ENVIRONMENT_FAILED".equals(msg)) {
                            dm.add(PermissionRequestHandler.requestRecordAudio(getActivity()).subscribe(() -> {
                            }, throwable -> ToastHelper.show(getActivity(), throwable.getLocalizedMessage())));

                        }
                    }
                }));
        if (chatView != null) {
            chatView.addListeners();
//            chatView.onLoadMore(0, 0);
        }

    }


    @Override
    public void clearData() {

    }

    public boolean hasVoice(User user) {
        return ChatSDK.thread().hasVoice(thread, user) && !thread.isReadOnly();
    }

    public void showOrHideTextInputView() {
        if (hasVoice(ChatSDK.currentUser())) {
            showTextInput();
        } else {
            hideTextInput();
        }
    }

    /**
     * Send text text
     *
     * @param text to send.
     */
    public void sendMessage(String text) {

        // Clear the draft text
        thread.setDraft(null);

        if (text == null || text.isEmpty()) {
            return;
        }

        String prompt = replyText.getText().toString().trim();

        if (replyView.getVisibility() == View.VISIBLE && !prompt.isEmpty()) {
            GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
            handleMessageSend(handler.sendExploreMessage(text, null, GWThreadHandler.action_input_prompt, prompt));
        } else {
            handleMessageSend(ChatSDK.thread().sendMessageWithText(text.trim(), thread));
        }
        hideReplyView();

    }

    protected void handleMessageSend(Completable completable) {
        completable.observeOn(RX.main()).doOnError(throwable -> {
            Log.e("sending", throwable.getLocalizedMessage());
            showToast(throwable.getLocalizedMessage());
        }).subscribe(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        AsrHelper.INSTANCE.stopAsr();

        removeUserFromChatOnExit = !ChatSDK.config().publicChatAutoSubscriptionEnabled;

        if (thread.typeIs(ThreadType.Public)) {
            User currentUser = ChatSDK.currentUser();
            ChatSDK.thread().addUsersToThread(thread, currentUser).subscribe();
        }

        // Show a local notification if the text is from a different thread
        ChatSDK.ui().setLocalNotificationHandler(thread -> !thread.getEntityID().equals(this.thread.getEntityID()));

//        if (audioBinder != null) {
//            audioBinder.updateRecordMode();
//        }

        if (!StringChecker.isNullOrEmpty(thread.getDraft())) {
            input.getInputEditText().setText(thread.getDraft());
        }

        // Put it here in the case that they closed the app with this screen open
//        thread.markReadAsync().subscribe();

        showOrHideTextInputView();


    }

    @Override
    public void onPause() {
        super.onPause();

        hideKeyboard();

        if (!StringChecker.isNullOrEmpty(input.getInputEditText().getText())) {
            thread.setDraft(input.getInputEditText().getText().toString());
        } else {
            thread.setDraft(null);
        }
    }

    /**
     * Sending a broadcast that the chat was closed, Only if there were new messageHolders on this chat.
     * This is used for example to update the thread list that messageHolders has been read.
     */
    @Override
    public void onStop() {
        super.onStop();
        doOnStop();
    }

    protected void doOnStop() {
        becomeInactive();

        if (thread != null && thread.typeIs(ThreadType.Public) && (removeUserFromChatOnExit || thread.isMuted())) {
            // Don't add this to activity disposable map because otherwise it can be cancelled before completion
            ChatSDK.events().disposeOnLogout(ChatSDK.thread()
                    .removeUsersFromThread(thread, ChatSDK.currentUser())
                    .observeOn(RX.main()).subscribe());
        }
    }

    /**
     * Not used, There is a piece of code here that could be used to clean all images that was loaded for this chat from cache.
     */
    @Override
    public void onDestroy() {
        if (enableTrace) {
            Debug.stopMethodTracing();
        }
        if (chatView != null) {
            chatView.removeListeners();
        }

        // TODO: Test this - in some situations where we are not using the
        // main activity this can be important
        ChatSDK.ui().setLocalNotificationHandler(thread -> true);

        super.onDestroy();
    }

    @Override
    public void onNewIntent(Thread thread) {
        if (true) {
            return;
        }
        this.thread = thread;
        clear();
        chatView.onLoadMore(0, 0);
    }

    public void switchContent() {
        if (chatView != null) {
//            chatView.switchContent();
        }
    }

    public void clear() {
        chatView.clear();
        chatView.removeListeners();
        dm.dispose();
        addListeners();
    }

    public void clearSelection() {
//        chatView.clearSelection();
//        updateOptionsButton();
    }

    /**
     * Open the thread details context, Admin user can change thread name an messageImageView there.
     */
    protected void openThreadDetailsActivity() {

        // We don't want to remove the user if we load another activity
        // Like the sticker activity
        removeUserFromChatOnExit = false;

        if (getActivity() != null) {
            ChatSDK.ui().startThreadDetailsActivity(getActivity(), thread.getEntityID());
        }
    }

    @Override
    public void sendAudio(final File file, String mimeType, long duration) {
        if (ChatSDK.audioMessage() != null && getActivity() != null) {
            handleMessageSend(ChatSDK.audioMessage().sendMessage(getActivity(), file, mimeType, duration, thread));
        }
    }

    public void startTyping() {
        setChatState(TypingIndicatorHandler.State.composing);
    }

    public void becomeInactive() {
        setChatState(TypingIndicatorHandler.State.inactive);
    }

    public void stopTyping() {
        setChatState(TypingIndicatorHandler.State.active);
    }

    protected void setChatState(TypingIndicatorHandler.State state) {
        if (ChatSDK.typingIndicator() != null) {
            ChatSDK.typingIndicator().setChatState(state, thread)
                    .observeOn(RX.main())
                    .doOnError(throwable -> {
                        System.out.println("Catch disconnected error");
                        //
                    })
                    .subscribe();
        }
    }


    public static InputMethodManager getInputMethodManager(Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void showKeyboard() {
        EditText et = input.getInputEditText();
        et.post(() -> {
            et.requestFocus();
            imm().showSoftInput(et, 0);
        });
    }

    public void hideKeyboard() {
        EditText et = input.getInputEditText();
        imm().hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    public InputMethodManager imm() {
        EditText et = input.getInputEditText();
        return getInputMethodManager(et.getContext());
    }

    public void toggleOptions(boolean show) {
        if (show) {
            showKeyboard();
        } else {
            hideKeyboard();
        }
//        // If the keyboard overlay is available
//        if (root.keyboardOverlayAvailable() && getActivity() != null) {
//            koh.toggle();
//        } else {
//            // We don't want to remove the user if we load another activity
//            // Like the sticker activity
//            removeUserFromChatOnExit = false;
//
//            if (getActivity() != null) {
//                optionsHandler = ChatSDK.ui().getChatOptionsHandler(this);
//                optionsHandler.show(getActivity());
//            }
//
//        }
    }

    @Override
    public void executeChatOption(ChatOption option) {
        if (getActivity() != null) {
            handleMessageSend(option.execute(getActivity(), launcher, thread));
        }
    }

    @Override
    public Thread getThread() {
        return thread;
    }


    public boolean onBackPressed() {
//        if (!chatView.getSelectedMessages().isEmpty()) {
//            chatView.clearSelection();
//            return true;
//        }
        // If the keyboard overlay is showing, we go back to the keyboard
        return koh.back();
    }

    @Override
    public void send(Sendable sendable) {
        if (getActivity() != null) {
            handleMessageSend(sendable.send(getActivity(), launcher, getThread()));
        }
    }

    @Override
    public void showOverlay(AbstractKeyboardOverlayFragment fragment) {
        koh.showOverlay(fragment);
    }

    @Override
    public boolean keyboardOverlayAvailable() {
        return getKeyboardAwareView().keyboardOverlayAvailable() && UIModule.config().keyboardOverlayEnabled;
    }


    protected void addTypingListener() {
        input.setTypingListener(new GWMsgInput.TypingListener() {
            @Override
            public void onStartTyping() {
                startTyping();
            }

            @Override
            public void onStopTyping() {
                stopTyping();
            }

            @Override
            public void onHeightChange() {

                updateChatViewMargins(true);
            }
        });
    }

    public KeyboardAwareFrameLayout getKeyboardAwareView() {
        return root;
    }

    public FragmentContainerView getKeyboardOverlay() {
        return keyboardOverlay;
    }

    @Override
    public void onSaveInstanceState(@io.reactivex.annotations.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(Keys.IntentKeyThreadEntityID, thread.getEntityID());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        restoreState(savedInstanceState);
    }

    public void restoreState(@Nullable Bundle savedInstanceState) {
        if (thread == null) {
            GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
            thread = handler.createChatSessions();
        }
        String threadEntityID = null;
        if (savedInstanceState != null) {
            threadEntityID = savedInstanceState.getString(Keys.IntentKeyThreadEntityID);
        }
        if (threadEntityID == null) {
            Bundle args = getArguments();
            if (args != null) {
                threadEntityID = args.getString(Keys.IntentKeyThreadEntityID);
            }
        }
        if (threadEntityID != null && thread == null) {
            thread = ChatSDK.db().fetchThreadWithEntityID(threadEntityID);
        }
    }

    public boolean isOverlayVisible() {
        return koh.keyboardOverlayVisible();
    }

    public boolean isOverlayOrKeyboardVisible() {
        return isOverlayVisible() || getKeyboardAwareView().isKeyboardOpen();
    }

    public boolean isOverlayVisible(String key) {
        if (key == null) {
            return false;
        }
        return isOverlayVisible() && key.equals(currentOverlayKey());
    }

    public String currentOverlayKey() {
        return koh.currentOverlay() != null ? koh.currentOverlay().key() : null;
    }


}
