package sdk.chat.demo.robot.activities;

import android.os.Bundle;

import androidx.drawerlayout.widget.DrawerLayout;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView;
import java.util.List;

import materialsearchview.MaterialSearchView;
import sdk.chat.core.dao.Thread;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.fragments.CozeChatFragment;
import sdk.chat.ui.activities.MainActivity;
import sdk.chat.ui.fragments.AbstractChatFragment;
import sdk.chat.ui.fragments.ChatFragment;

public class CozeMainActivity extends MainActivity {
    protected List<Thread> threads;
    protected AbstractChatFragment chatFragment;
    private DrawerLayout drawer;
    private MaterialDrawerSliderView slider;

    // 静态菜单项ID
    private static final long NAV_HOME = 1L;
    private static final long NAV_PROFILE = 2L;
    private static final long NAV_SETTINGS = 3L;

    @Override
    protected int getLayout() {
        return R.layout.activity_main_coze_drawer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        updateThread(savedInstanceState);

        if (savedInstanceState == null) {
            chatFragment = new CozeChatFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, chatFragment).commit();
        } else {
            chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }

    }

    @Override
    protected boolean searchEnabled() {
        return false;
    }

    @Override
    protected void search(String text) {

    }

    @Override
    protected MaterialSearchView searchView() {
        return null;
    }

    @Override
    protected void reloadData() {

    }

    @Override
    protected void clearData() {

    }

    @Override
    protected void updateLocalNotificationsForTab() {

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
