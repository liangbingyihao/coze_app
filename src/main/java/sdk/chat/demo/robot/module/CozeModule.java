package sdk.chat.demo.robot.module;

import android.content.Context;

import app.xmpp.adapter.R;
import app.xmpp.adapter.module.XMPPConfig;
import sdk.chat.core.base.BaseNetworkAdapter;
import sdk.chat.core.interfaces.InterfaceAdapter;
import sdk.chat.core.module.AbstractModule;
import sdk.chat.core.session.Configure;
import sdk.chat.core.session.InterfaceAdapterProvider;
import sdk.chat.core.session.NetworkAdapterProvider;
import sdk.chat.demo.robot.ui.CozeInterfaceAdapter;
import sdk.chat.ui.module.UIConfig;
import sdk.chat.ui.module.UIModule;

/**
 * Created by benjaminsmiley-andrews on 12/07/2017.
 */

public class CozeModule extends AbstractModule implements NetworkAdapterProvider, InterfaceAdapterProvider {

    protected static final CozeModule instance = new CozeModule();

    public static CozeModule shared() {
        return instance;
    }

    /**
     * @see XMPPConfig
     * @return configuration object
     */
    public static CozeConfig<CozeModule> builder() {
        return instance.config;
    }

    public CozeModule configureUI(Configure<UIConfig> configure) throws Exception {
        configure.with(UIModule.config());
        return instance;
    }

    public CozeConfig<CozeModule> config = new CozeConfig<>(this);

    @Override
    public void activate(Context context) {
//        if (UIModule.config().usernameHint == null) {
//            UIModule.config().usernameHint = context.getString(R.string.user_jid);
//        }
        UIModule.config().customizeGroupImageEnabled = false;
//        UIModule.config().messageReplyEnabled = false;
//        UIModule.config().messageForwardingEnabled = false;

//        ChatSDK.hook().addHook(Hook.sync(data -> {
//            OMEMOModule.shared().start();
//        }), HookEvent.DidAuthenticate);

    }

    public void setupOmemo() {


    }

    @Override
    public Class<? extends InterfaceAdapter> getInterfaceAdapter() {
        return CozeInterfaceAdapter.class;
    }

    @Override
    public Class<? extends BaseNetworkAdapter> getNetworkAdapter() {
        return config.networkAdapter;
    }

    public static CozeConfig config() {
        return shared().config;
    }

    @Override
    public void stop() {
        config = new CozeConfig<>(this);
    }

}
