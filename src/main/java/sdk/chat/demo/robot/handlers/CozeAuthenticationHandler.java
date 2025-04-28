package sdk.chat.demo.robot.handlers;

import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.xmpp.adapter.R;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.base.AbstractAuthenticationHandler;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.hook.HookEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.core.utils.KeyStorage;
import sdk.chat.demo.robot.CozeApiManager;
import sdk.chat.demo.robot.extensions.DeviceIdHelper;
import sdk.guru.common.RX;


/**
 * Created by benjaminsmiley-andrews on 01/07/2017.
 */

public class CozeAuthenticationHandler extends AbstractAuthenticationHandler {
    public CozeAuthenticationHandler() {
    }

    @Override
    public Boolean accountTypeEnabled(AccountDetails.Type type) {
        return type == AccountDetails.Type.Username || type == AccountDetails.Type.Register
                || type == AccountDetails.Type.Anonymous;
    }

    @Override
    public Completable authenticate() {
        return Completable.defer(() -> {

            if (isAuthenticatedThisSession() || isAuthenticated()) {
                return Completable.complete();
            }
            if (!isAuthenticating()) {
                AccountDetails details = cachedAccountDetails();
                return authenticate(details);
//                if (details.type == AccountDetails.Type.Custom || details.areValid()) {
//                    return authenticate(details);
//                } else {
//                    return Completable.error(new Exception());
//                }
            }
            return authenticating;
        });
    }

    @Override
    public Completable authenticate(final AccountDetails details) {
        return Completable.defer(() -> {

//            String database = details.username + XMPPManager.getCurrentServer(ChatSDK.ctx()).domain;
//            ChatSDK.db().openDatabase(database);

//            if (isAuthenticatedThisSession() || isAuthenticated()) {
////                return Completable.error(ChatSDK.getException(R.string.already_authenticated));
//                return loginSuccessful(details);
//            }
            if (!isAuthenticating()) {
                authenticating = CozeApiManager.shared().authenticate(details).flatMapCompletable(this::loginSuccessful).cache();
            }
            return authenticating;
        }).doFinally(this::cancel);
    }

    protected Completable loginSuccessful(AccountDetails details) {
        return Completable.defer(() -> {
//            String userId = details.getMetaValue("userId");
            String userId = "user_" + details.getMetaValue("userId");
            ChatSDK.db().openDatabase(userId);
            if(details.type==AccountDetails.Type.Username){
                ChatSDK.shared().getKeyStorage().save(details.username, details.password);
            }
            setCurrentUserEntityID(userId);

            User user = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, userId);
            List<User> robot = ChatSDK.contact().contacts();
            if (!robot.isEmpty()) {
                user.addContact(robot.get(0));
                ChatSDK.db().update(user);
            }

            if (ChatSDK.hook() != null) {
                HashMap<String, Object> data = new HashMap<>();
                data.put(HookEvent.User, user);
                ChatSDK.hook().executeHook(HookEvent.DidAuthenticate, data).subscribe(ChatSDK.events());
            }


            ChatSDK.core().sendAvailablePresence().subscribe();

            Logger.info("Authentication complete! name = " + user.getName() + ", id = " + user.getEntityID());

            setAuthStateToIdle();
            return Completable.complete();
        });
    }

    public AccountDetails cachedAccountDetails() {
        AccountDetails accountDetails = AccountDetails.username(ChatSDK.shared().getKeyStorage().get(KeyStorage.UsernameKey), ChatSDK.shared().getKeyStorage().get(KeyStorage.PasswordKey));
        if(!accountDetails.areValid()){
            accountDetails = AccountDetails.token(DeviceIdHelper.INSTANCE.getDeviceId(ChatSDK.ctx()));
        }
       return accountDetails;
    }

    public Boolean cachedCredentialsAvailable() {
        return true;
    }

    @Override
    public Boolean isAuthenticated() {
//        XMPPConnection connection = XMPPManager.shared().getConnection();
        return CozeApiManager.shared().isAuthenticated();
    }

    @Override
    public Completable logout() {
        return Completable.create(emitter -> {

            ChatSDK.events().source().accept(NetworkEvent.logout());

            CozeApiManager.shared().logout();

            clearCurrentUserEntityID();
            ChatSDK.shared().getKeyStorage().clear();

            ChatSDK.db().closeDatabase();

            emitter.onComplete();
        }).subscribeOn(RX.computation());
    }

    // TODO: Implement this
    @Override
    public Completable changePassword(String email, String oldPassword, String newPassword) {
        return Completable.create(emitter -> {
//            XMPPManager.shared().accountManager().changePassword(newPassword);
            emitter.onComplete();
        }).subscribeOn(RX.io());
    }

    @Override
    public Completable sendPasswordResetMail(String email) {
        return Completable.error(new Throwable("Password email not supported"));
    }

}
