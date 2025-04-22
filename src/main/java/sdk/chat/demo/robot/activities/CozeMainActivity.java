package sdk.chat.demo.robot.activities;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import io.reactivex.Single;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.StringChecker;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.activities.ChatActivity;
import sdk.chat.ui.activities.LoginActivity;
import sdk.chat.ui.extras.MainDrawerActivity;
import sdk.chat.ui.fragments.AbstractChatFragment;
import sdk.chat.ui.fragments.ChatFragment;
import sdk.guru.common.RX;

public class CozeMainActivity extends MainDrawerActivity {
    protected List<Thread> threads;
    protected AbstractChatFragment chatFragment;

    @Override
    protected int getLayout() {
        return R.layout.activity_main_coze_drawer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        updateThread(savedInstanceState);

//        if (savedInstanceState == null) {
//            chatFragment = ChatSDKUI.getChatFragment(thread);
//            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, chatFragment).commit();
//        } else {
//            chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//        }

    }
//
//    public void updateThread(Bundle bundle) {
//        thread = null;
//
//        if (bundle == null) {
//            bundle = getIntent().getExtras();
//        }
//
//        if (bundle != null && bundle.containsKey(Keys.IntentKeyThreadEntityID)) {
//            String threadEntityID = bundle.getString(Keys.IntentKeyThreadEntityID);
//            if (threadEntityID != null) {
//                thread = ChatSDK.db().fetchThreadWithEntityID(threadEntityID);
//            }
//        } else {
//            threads = ChatSDK.thread().getThreads(ThreadType.Private);
//            thread = threads.get(0);
//        }
//
//        if (thread == null) {
//            finish();
//        }
//
//    }

}
