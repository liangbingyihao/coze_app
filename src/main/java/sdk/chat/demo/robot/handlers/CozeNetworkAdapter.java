package sdk.chat.demo.robot.handlers;

import sdk.chat.core.base.BaseNetworkAdapter;
import sdk.chat.demo.robot.wrappers.GWEventHandler;
//import sdk.chat.firebase.adapter.FirebaseContactHandler;
//import sdk.chat.firebase.adapter.FirebaseCoreHandler;
//import sdk.chat.firebase.adapter.FirebasePublicThreadHandler;
//import sdk.chat.firebase.adapter.FirebaseSearchHandler;
//import sdk.guru.realtime.RealtimeReferenceManager;

public class CozeNetworkAdapter extends BaseNetworkAdapter {

    public CozeNetworkAdapter () {
        events = new GWEventHandler();
//        core = new FirebaseCoreHandler();
        auth = new CozeAuthenticationHandler();
        thread = new CozeThreadHandler();
//        publicThread = new FirebasePublicThreadHandler();
//        search = new FirebaseSearchHandler();
        contact = new CozeContactHandler();
    }

    public void stop() {
        super.stop();
//        RealtimeReferenceManager.shared().removeAllListeners();
    }

}