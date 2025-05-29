package sdk.chat.demo.robot.handlers;

import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import sdk.chat.core.base.AbstractAuthenticationHandler;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.hook.HookEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.core.utils.KeyStorage;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.extensions.DeviceIdHelper;
import sdk.guru.common.RX;


/**
 * Created by benjaminsmiley-andrews on 01/07/2017.
 */

public class GWAuthenticationHandler extends AbstractAuthenticationHandler {
    public GWAuthenticationHandler() {
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
            if (!isAuthenticating()) {
                authenticating = GWApiManager.shared().authenticate(details).flatMapCompletable(this::loginSuccessful).cache();
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
            GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
            handler.getWelcomeMsg().subscribe();
            handler.createChatSessions();

            if (ChatSDK.hook() != null) {
                HashMap<String, Object> data = new HashMap<>();
                data.put(HookEvent.User, user);
                ChatSDK.hook().executeHook(HookEvent.DidAuthenticate, data).subscribe(ChatSDK.events());
            }


//            ChatSDK.core().sendAvailablePresence().subscribe();
//            ChatSDK.db().getDaoCore().getDaoSession().getMessageDao().queryBuilder()
//                    .where(MessageDao.Properties.Type.eq(MessageType.System))
//                    .buildDelete()
//                    .executeDeleteWithoutDetachingEntities();

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
        return GWApiManager.shared().isAuthenticated();
    }

    @Override
    public Completable logout() {
        return Completable.create(emitter -> {

            ChatSDK.events().source().accept(NetworkEvent.logout());

            GWApiManager.shared().logout();

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
