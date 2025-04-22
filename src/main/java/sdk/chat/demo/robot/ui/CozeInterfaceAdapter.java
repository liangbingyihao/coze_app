package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import sdk.chat.core.Tab;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.CozeLoginActivity;
import sdk.chat.demo.robot.fragments.CozeChatFragment;
import sdk.chat.ui.BaseInterfaceAdapter;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.fragments.ContactsFragment;
import sdk.chat.ui.module.UIModule;

/**
 * Created by benjaminsmiley-andrews on 12/07/2017.
 */

public class CozeInterfaceAdapter extends BaseInterfaceAdapter {
    protected Fragment chatFragment = new CozeChatFragment();

    public CozeInterfaceAdapter() {

    }

    public CozeInterfaceAdapter(Context context) {
        super(context);
//        searchActivity = XMPPSearchActivity.class;
        loginActivity = CozeLoginActivity.class;
    }

//    @Override
//    public List<Tab> defaultTabs() {
//
//        ArrayList<Tab> tabs = new ArrayList<>();
//
//        tabs.add(privateThreadsTab());
//        tabs.add(contactsTab());
//
//        return tabs;
//    }

    @Override
    public Tab contactsTab() {
        if (contactsTab == null) {
            contactsTab = new Tab(context.get().getString(sdk.chat.ui.R.string.contacts), ChatSDKUI.icons().get(context.get(), ChatSDKUI.icons().contact, ChatSDKUI.icons().tabIconColor), contactsFragment());
        }
        return contactsTab;
    }

    @Override
    public List<Tab> defaultTabs() {
        ArrayList<Tab> tabs = new ArrayList<>();
        tabs.add(privateThreadsTab());
//        tabs.add(contactsTab());
//        if (UIModule.config().publicRoomRoomsEnabled) {
//            tabs.add(publicThreadsTab());
//        }
        return tabs;
    }


    @Override
    public Tab privateThreadsTab() {
        if (privateThreadsTab == null) {
            privateThreadsTab = new Tab(String.format(context.get().getString(R.string.conversations)), ChatSDKUI.icons().get(context.get(), ChatSDKUI.icons().chat, ChatSDKUI.icons().tabIconColor), chatFragment);
        }
        return privateThreadsTab;
    }

}
