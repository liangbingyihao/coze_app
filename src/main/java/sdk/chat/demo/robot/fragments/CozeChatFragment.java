package sdk.chat.demo.robot.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Debug;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.handlers.TypingIndicatorHandler;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.fragments.ChatFragment;
import sdk.chat.ui.module.UIModule;

public class CozeChatFragment extends ChatFragment {
    private DataCallback dataCallback;

    public interface DataCallback {
        Thread getCurrentData();
        void onDataUpdated(Thread thread);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_coze_chat;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataCallback) {
            dataCallback = (DataCallback) context;
            this.thread = dataCallback.getCurrentData();
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DataCallback");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        dataCallback = null;
    }

//    public void setThread(Thread thread) {
//        if(thread==null){
//            thread = ChatSDK.thread().getThreads(ThreadType.Private).get(0);
//        }
//        this.thread = thread;
//        Bundle bundle = new Bundle();
//        bundle.putString(Keys.IntentKeyThreadEntityID, thread.getEntityID());
//        setArguments(bundle);
//    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        if(thread==null){
//            setThread(null);
//        }
//        return super.onCreateView(inflater, container, savedInstanceState);
//    }


    protected void initViews() {

        chatActionBar = getActivity().findViewById(R.id.chatActionBar);
        chatView = rootView.findViewById(sdk.chat.ui.R.id.chatView);
        divider = rootView.findViewById(sdk.chat.ui.R.id.divider);
        replyView = rootView.findViewById(sdk.chat.ui.R.id.replyView);
        input = rootView.findViewById(sdk.chat.ui.R.id.input);
        listContainer = rootView.findViewById(sdk.chat.ui.R.id.listContainer);
        searchView = getActivity().findViewById(R.id.searchView);
        root = rootView.findViewById(sdk.chat.ui.R.id.root);
        messageInputLinearLayout = rootView.findViewById(sdk.chat.ui.R.id.messageInputLinearLayout);
        keyboardOverlay = rootView.findViewById(sdk.chat.ui.R.id.keyboardOverlay);

        chatView.setDelegate(this);

        chatActionBar.onSearchClicked(v -> {
            searchView.showSearch();
        });

        addOnSearchListener();

        chatView.initViews();

        if (UIModule.config().messageSelectionEnabled) {
            chatView.enableSelectionMode(count -> {
                invalidateOptionsMenu();
                updateOptionsButton();
            });
        }

        if (!hasVoice(ChatSDK.currentUser())) {
            hideTextInput();
        }

        if (ChatSDK.audioMessage() != null && getActivity() != null) {
            addAudioBinder();
        } else {
            input.setInputListener(input -> {
                sendMessage(String.valueOf(input));
                return true;
            });
        }

        addTypingListener();

        input.setAttachmentsListener(this::toggleOptions);

        replyView.setOnCancelListener(v -> hideReplyView());

        // Action bar
        chatActionBar.setOnClickListener(v -> {
            chatActionBar.setEnabled(false);
            openThreadDetailsActivity();
        });

        chatActionBar.reload(thread);

        setChatState(TypingIndicatorHandler.State.active);

        if (enableTrace) {
            Debug.startMethodTracing("chat");
        }

        addListeners();


    }

}
