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
            user.setAvatarURL("data:image/png;base64,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");

            User robot = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, "robot1");
            robot.setName("点滴见证");
            robot.setHeaderURL("https://media.istockphoto.com/id/1003676294/zh/%E5%90%91%E9%87%8F/%E5%9C%96%E7%95%AB%E5%9F%BA%E7%9D%A3%E5%BE%92%E5%8D%81%E5%AD%97%E6%9E%B6%E5%BE%A9%E6%B4%BB%E7%AF%80%E8%83%8C%E6%99%AF%E5%9C%A8%E9%87%91%E5%AD%90%E5%8F%A3%E6%B0%A3.jpg?s=2048x2048&w=is&k=20&c=ZQNgr7w53jaLIKx_YousXMgTZrv5ycqdThyejAdaqXg=");
//            robot.setAvatarURL("data:image/png;base64,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");
            user.addContact(robot);
            user.setName("小羊");
            ChatSDK.db().update(user);

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
