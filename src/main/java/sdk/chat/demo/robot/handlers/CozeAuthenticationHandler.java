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
import sdk.guru.common.RX;


/**
 * Created by benjaminsmiley-andrews on 01/07/2017.
 */

public class CozeAuthenticationHandler extends AbstractAuthenticationHandler {
    public CozeAuthenticationHandler() {
    }

    @Override
    public Boolean accountTypeEnabled(AccountDetails.Type type) {
        return type == AccountDetails.Type.Username || type == AccountDetails.Type.Register;
    }

    @Override
    public Completable authenticate() {
        return Completable.defer(() -> {

            if (isAuthenticatedThisSession() || isAuthenticated()) {
                return Completable.complete();
            }
            if (!isAuthenticating()) {
                AccountDetails details = cachedAccountDetails();
                if (details.areValid()) {
                    return authenticate(details);
                } else {
                    return Completable.error(new Exception());
                }
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
//                authenticating = Single.create((SingleOnSubscribe<AccountDetails>) emitter -> {
//
//                    switch (details.type) {
//                        case Username:
//                            Map<String, String> params = new HashMap<>();
//                            params.put("username", details.username);
//                            params.put("password", details.password);
//                            Type typeObject = new TypeToken<HashMap>() {
//                            }.getType();
//                            String gsonData = gson.toJson(params, typeObject);
//                            RequestBody body = RequestBody.create(
//                                    gsonData,
//                                    MediaType.parse("application/json; charset=utf-8")
//                            );
//
//                            Request request = new Request.Builder()
//                                    .url("http://8.217.172.116:5000/api/auth/login")
//                                    .post(body)
//                                    .build();
//
//                            client.newCall(request).enqueue(new Callback() {
//                                @Override
//                                public void onFailure(Call call, IOException error) {
//                                    System.err.println("请求失败: " + error.getMessage());
//                                    emitter.onError(error);
//                                }
//
//                                @Override
//                                public void onResponse(Call call, Response response) throws IOException {
//                                    String responseBody = response.body().string();
//                                    Type type = new TypeToken<Map<String, Object>>() {}.getType();
//                                    Map<String, Object> map = gson.fromJson(responseBody, type);
//                                    Map<String,String> data = (Map<String, String>) map.get("data");
//                                    details.token = data.get("access_token");
//                                    details.setMetaValue("userId", String.valueOf(data.get("user_id")));
//                                    emitter.onSuccess(details);
//                                }
//                            });
//                            break;
//
////                            return XMPPManager.shared().login(details.username, details.password).andThen(Completable.defer(() -> {
////                                return loginSuccessful(details);
////                            })).doOnError(throwable -> {
//////                                setAuthStateToIdle();
////                                clearCurrentUserEntityID();
////                            }).doFinally(this::setAuthStateToIdle);
//                        case Register:
////                            return XMPPManager.shared().register(details.username, details.password).andThen(Completable.defer(() -> {
////                                return loginSuccessful(details);
////                            })).doOnError(throwable -> {
//////                                setAuthStateToIdle();
////                                clearCurrentUserEntityID();
////                            }).doFinally(this::setAuthStateToIdle);
//                        default:
//                            emitter.onError(ChatSDK.getException(sdk.chat.firebase.adapter.R.string.no_login_type_defined));
//                    }
//                }).subscribeOn(RX.io()).flatMapCompletable(this::loginSuccessful).cache();
            }
            return authenticating;
        }).doFinally(this::cancel);
    }

    protected Completable loginSuccessful(AccountDetails details) {
        return Completable.defer(() -> {
//            String userId = details.getMetaValue("userId");
            String userId = "user_"+details.getMetaValue("userId");
            ChatSDK.db().openDatabase(userId);
            ChatSDK.shared().getKeyStorage().save(details.username, details.password);

            setCurrentUserEntityID(userId);

            User user = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, userId);

            User robot = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, "robot1");
            user.addContact(robot);

//            ChatSDK.events().impl_currentUserOn(user.getEntityID());

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
        return AccountDetails.username(ChatSDK.shared().getKeyStorage().get(KeyStorage.UsernameKey), ChatSDK.shared().getKeyStorage().get(KeyStorage.PasswordKey));
    }

    public Boolean cachedCredentialsAvailable() {
        return cachedAccountDetails().areValid();
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
