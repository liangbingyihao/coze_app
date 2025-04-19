package sdk.chat.demo.robot.ui;

import android.content.Context;

import sdk.chat.demo.robot.activities.CozeLoginActivity;
import sdk.chat.ui.BaseInterfaceAdapter;

/**
 * Created by benjaminsmiley-andrews on 12/07/2017.
 */

public class CozeInterfaceAdapter extends BaseInterfaceAdapter {

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


}
