package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.functions.Function;
import sdk.chat.core.base.AbstractThreadHandler;
import sdk.chat.core.dao.CachedFile;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.User;
import sdk.chat.core.dao.UserThreadLink;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.core.utils.Debug;
import sdk.chat.demo.robot.CozeApiManager;
import sdk.chat.demo.robot.wrappers.MessageWrapper;
import sdk.chat.firebase.adapter.FirebasePaths;
import sdk.chat.firebase.adapter.moderation.Permission;
import sdk.chat.firebase.adapter.module.FirebaseModule;
import sdk.guru.common.RX;
import sdk.guru.realtime.RXRealtime;

/**
 * Created by benjaminsmiley-andrews on 25/05/2017.
 */

public class CozeThreadHandler extends AbstractThreadHandler {

    @Override
    public Single<List<Message>> loadMoreMessagesAfter(Thread thread, @Nullable Date after, boolean loadFromServer) {
        return super.loadMoreMessagesAfter(thread, after, loadFromServer).flatMap(localMessages -> {
            // If we have messages
            // If we did load some messages locally, update the from date to the last of those messages
            Date finalAfterDate = localMessages.size() > 0 ? localMessages.get(0).getDate() : after;

            return FirebaseModule.config().provider.threadWrapper(thread).loadMoreMessagesAfter(finalAfterDate, 0).map((Function<List<Message>, List<Message>>) remoteMessages -> {

                ArrayList<Message> mergedMessages = new ArrayList<>(localMessages);
                mergedMessages.addAll(remoteMessages);

//                if (ChatSDK.encryption() != null) {
//                    for (Message m : mergedMessages) {
//                        ChatSDK.encryption().decrypt(m);
//                    }
//                }

                Debug.messageList(mergedMessages);

                return mergedMessages;
            });
        });
    }

    public Single<List<Message>> loadMoreMessagesBefore(final Thread thread, final Date fromDate, boolean loadFromServer) {
        return super.loadMoreMessagesBefore(thread, fromDate, loadFromServer).flatMap(localMessages -> {

            int messageToLoad = ChatSDK.config().messagesToLoadPerBatch;
            int localMessageSize = localMessages.size();

            if (localMessageSize < messageToLoad && loadFromServer) {
                // If we did load some messages locally, update the from date to the last of those messages
                Date finalFromDate = localMessageSize > 0 ? localMessages.get(localMessageSize - 1).getDate() : fromDate;

                return FirebaseModule.config().provider.threadWrapper(thread).loadMoreMessagesBefore(finalFromDate, messageToLoad).map((Function<List<Message>, List<Message>>) remoteMessages -> {

                    ArrayList<Message> mergedMessages = new ArrayList<>(localMessages);
                    mergedMessages.addAll(remoteMessages);

//                    if (ChatSDK.encryption() != null) {
//                        for (Message m : mergedMessages) {
//                            ChatSDK.encryption().decrypt(m);
//                        }
//                    }

                    Debug.messageList(mergedMessages);

                    return mergedMessages;
                });
            }
            return Single.just(localMessages);
        });
    }

    /**
     * Add given users list to the given thread.
     * The RepetitiveCompletionListenerWithError will notify by his "onItem" method for each user that was successfully added.
     * In the "onItemFailed" you can get all users that the system could not add to the server.
     * When all users are added the system will call the "onDone" method.
     **/
    public Completable addUsersToThread(final Thread thread, final List<User> users) {
        return FirebaseModule.config().provider.threadWrapper(thread).addUsers(users);
    }

    @Override
    public boolean canAddUsersToThread(Thread thread) {
        if (thread.typeIs(ThreadType.PrivateGroup)) {
            String role = roleForUser(thread, ChatSDK.currentUser());
            return Permission.isOr(role, Permission.Owner, Permission.Admin);
        }
        return false;
    }

    public Completable mute(Thread thread) {
        return Completable.defer(() -> {
            DatabaseReference threadUsersRef = FirebasePaths.threadUsersRef(thread.getEntityID()).child(ChatSDK.currentUserID()).child(Keys.Mute);
            RXRealtime realtime = new RXRealtime();
            return realtime.set(threadUsersRef, true);
        });
    }

    public Completable unmute(Thread thread) {
        return Completable.defer(() -> {
            DatabaseReference threadUsersRef = FirebasePaths.threadUsersRef(thread.getEntityID()).child(ChatSDK.currentUserID()).child(Keys.Mute);
            RXRealtime realtime = new RXRealtime();
            return realtime.set(threadUsersRef, false);
        });
    }

    public Completable removeUsersFromThread(final Thread thread, List<User> users) {
        return FirebaseModule.config().provider.threadWrapper(thread).removeUsers(users);
    }

    public Completable pushThread(Thread thread) {
        return FirebaseModule.config().provider.threadWrapper(thread).push();
    }

    public Completable pushThreadMeta(Thread thread) {
        return FirebaseModule.config().provider.threadWrapper(thread).pushMeta();
    }

    @Override
    public boolean muteEnabled(Thread thread) {
        return true;
    }

    /**
     * Send a text,
     * The text need to have a owner thread attached to it or it cant be added.
     * If the destination thread is public the system will add the user to the text thread if needed.
     * The uploading to the server part can bee seen her {@see FirebaseCoreAdapter#PushMessageWithComplition}.
     */
    public Completable sendMessage(final Message message) {
        return  CozeApiManager.shared().askRobot(message)
                .subscribeOn(RX.io()).flatMap(data -> {
                    message.setEntityID(data.get("id").getAsString());
                    ChatSDK.db().update(message);
                    pushForMessage(message);
                    return Single.just(message);
                }).ignoreElement();

//        return new MessageWrapper(message).send().andThen(new Completable() {
//            @Override
//            protected void subscribeActual(CompletableObserver observer) {
//                pushForMessage(message);
//                observer.onComplete();
//            }
//        });
    }

    @SuppressLint("CheckResult")
    public Single<Thread> createThread(String name, List<User> theUsers, int type, String entityID, String imageURL, Map<String, Object> meta) {

        if (entityID != null) {
            Thread t = ChatSDK.db().fetchThreadWithEntityID(entityID);
            if (t != null) {
                return Single.just(t);
            }
        }

        return CozeApiManager.shared().saveSession("1")
                .subscribeOn(RX.io()).flatMap(data -> {
                    final Thread thread = ChatSDK.db().createEntity(Thread.class);
                    thread.setEntityID(data.get("id").getAsString());
                    thread.setCreator(ChatSDK.currentUser());
                    thread.setCreationDate(new Date());
                    if (name != null) {
                        thread.setName(name, false);
                    }

                    if (imageURL != null) {
                        thread.setImageUrl(imageURL, false);
                    }

                    ArrayList<User> users = new ArrayList<>(theUsers);
                    User currentUser = ChatSDK.currentUser();

                    if (!users.contains(currentUser)) {
                        users.add(currentUser);
                    }
                    thread.addUsers(users);
                    thread.setType(ThreadType.Private1to1);
                    ChatSDK.db().update(thread);
                    return Single.just(thread);
                });
    }

    public Completable deleteThread(Thread thread) {
        return Completable.defer(() -> {
            return FirebaseModule.config().provider.threadWrapper(thread).deleteThread()
                    // Added to make sure thread removed (bug report from Dcom)
                    .andThen(super.deleteThread(thread));
        });
    }

    protected void pushForMessage(final Message message) {
        if (ChatSDK.push() != null && message.getThread().typeIs(ThreadType.Private)) {
            Map<String, Object> data = ChatSDK.push().pushDataForMessage(message);
            ChatSDK.push().sendPushNotification(data);
        }
    }

    public Completable deleteMessage(Message message) {
        return Completable.defer(() -> {
            if (message.getSender().isMe() && message.getMessageStatus().equals(MessageSendStatus.Sent) && !message.getMessageType().is(MessageType.System)) {
                // If possible delete the files associated with this message

                List<CachedFile> files = ChatSDK.db().fetchFilesWithIdentifier(message.getEntityID());
                for (CachedFile file : files) {
                    if (file.getRemotePath() != null) {
                        ChatSDK.upload().deleteFile(file.getRemotePath()).subscribe();
                    }
                    ChatSDK.db().delete(file);
                }

                // TODO: Can we do this with cached files?
//                MessagePayload payload = ChatSDK.getMessagePayload(message);
//                if (payload != null) {
//                    List<String> paths = payload.remoteURLs();
//                    for (String path: paths) {
//                        ChatSDK.upload().deleteFile(path).subscribe();
//                    }
//                }

                return FirebaseModule.config().provider.messageWrapper(message).delete();
            }
            message.getThread().removeMessage(message);
            return Completable.complete();
        });
    }

    @Override
    public boolean canDeleteMessage(Message message) {

        // We do it this way because otherwise when we exceed the number of messages,
        // This event is triggered as the messages go out of scope
        if (message.getThread().getCanDeleteMessagesFrom()==null || message.getDate().getTime() < message.getThread().getCanDeleteMessagesFrom().getTime()) {
            return false;
        }

        User currentUser = ChatSDK.currentUser();
        Thread thread = message.getThread();

        if (rolesEnabled(thread) && !thread.typeIs(ThreadType.Public)) {
            String role = roleForUser(thread, currentUser);
            int level = Permission.level(role);

            if (level > Permission.level(Permission.Member)) {
                return true;
            }
            if (level < Permission.level(Permission.Member)) {
                return false;
            }
        }

        if (isModerator(thread, currentUser)) {
            return true;
        }

        if (!hasVoice(thread, currentUser)) {
            return false;
        }

        if (message.getSender().isMe()) {
            return true;
        }

        return false;
    }

    public Completable leaveThread(Thread thread) {
        return Completable.defer(() -> FirebaseModule.config().provider.threadWrapper(thread).leave());
    }

    @Override
    public boolean canLeaveThread(Thread thread) {
        if (thread.typeIs(ThreadType.PrivateGroup)) {
            // Get the link
            String role = roleForUser(thread, ChatSDK.currentUser());
            return Permission.isOr(role, Permission.Owner, Permission.Admin, Permission.Member, Permission.Watcher);
        }
        return false;
    }

    public Completable joinThread(Thread thread) {
        return null;
    }

    @Override
    public boolean canJoinThread(Thread thread) {
        return false;
    }

    @Override
    public boolean rolesEnabled(Thread thread) {
        return thread.typeIs(ThreadType.Group);
    }

    @Override
    public boolean canChangeRole(Thread thread, User user) {
        if (!ChatSDK.config().rolesEnabled || !thread.typeIs(ThreadType.Group)) {
            return false;
        }

        String myRole = roleForUser(thread, ChatSDK.currentUser());
        String role = roleForUser(thread, user);

        // We need to have a higher permission level than them
        if (Permission.level(myRole) > Permission.level(role)) {
            return Permission.isOr(myRole, Permission.Owner, Permission.Admin);
        }
        return false;
    }

    @Override
    public String roleForUser(Thread thread, @NonNull User user) {
        UserThreadLink link = thread.getUserThreadLink(user.getId());
        if (link != null && link.hasLeft()) {
            return Permission.None;
        }
        if (user.equalsEntity(thread.getCreator())) {
            return Permission.Owner;
        }
        String role = thread.getPermission(user.getEntityID());
        if (role == null) {
            role = Permission.Member;
        }
        return role;
    }

    @Override
    public Completable setRole(final String role, Thread thread, User user) {
        return Completable.defer(() -> {
//            role = Permission.fromLocalized(role);
            return FirebaseModule.config().provider.threadWrapper(thread).setPermission(user.getEntityID(), role);
        });
    }

    @Override
    public List<String> availableRoles(Thread thread, User user) {
        List<String> roles = new ArrayList<>();

        String myRole = roleForUser(thread, ChatSDK.currentUser());

        for (String role : Permission.all()) {
            if (Permission.level(myRole) > Permission.level(role)) {
                roles.add(role);
            }
        }

        // In public chats it doesn't make sense to ban a user because
        // the data is public. They can be made a watcher instead
        if (thread.typeIs(ThreadType.Public)) {
            roles.remove(Permission.Banned);
        }

        return roles;
    }

    @Override
    public String localizeRole(String role) {
        return Permission.toLocalized(role);
    }

    // Moderation
    @Override
    public Completable grantVoice(Thread thread, User user) {
        return Completable.complete();
    }

    @Override
    public Completable revokeVoice(Thread thread, User user) {
        return Completable.complete();
    }

    @Override
    public boolean hasVoice(Thread thread, User user) {
        if (thread.containsUser(user) || thread.typeIs(ThreadType.Public)) {
            String role = roleForUser(thread, user);
            return Permission.isOr(role, Permission.Owner, Permission.Admin, Permission.Member);
        }
        return false;
    }

    @Override
    public boolean canChangeVoice(Thread thread, User user) {
        return false;
    }

    @Override
    public Completable grantModerator(Thread thread, User user) {
        return Completable.complete();
    }

    @Override
    public Completable revokeModerator(Thread thread, User user) {
        return Completable.complete();
    }

    @Override
    public boolean canChangeModerator(Thread thread, User user) {
        return false;
    }

    @Override
    public boolean isModerator(Thread thread, User user) {
        return false;
    }

    @Override
    public boolean isBanned(Thread thread, User user) {
        if (thread.containsUser(user) || thread.typeIs(ThreadType.Public)) {
            String role = thread.getPermission(user);
            return Permission.isOr(role, Permission.Banned);
        }
        return false;
    }

    @Override
    public String generateNewMessageID(Thread thread) {
        // User Firebase to generate an ID
        return FirebasePaths.threadMessagesRef(thread.getEntityID()).push().getKey();
    }

    @Override
    public Message newMessage(int type, Thread thread, boolean notify) {
        Message message = super.newMessage(type, thread, notify);
        ChatSDK.db().update(message);
        return message;
    }

    @Override
    public boolean canDestroy(Thread thread) {
        return false;
    }

    @Override
    public Completable destroy(Thread thread) {
        return Completable.complete();
    }

    @Override
    public boolean canEditThreadDetails(Thread thread) {
        if (thread.typeIs(ThreadType.Group)) {
            String role = roleForUser(thread, ChatSDK.currentUser());
            return Permission.isOr(role, Permission.Owner, Permission.Admin);
        }
        return false;
    }

    @Override
    public boolean canRemoveUserFromThread(Thread thread, User user) {
        if (thread.typeIs(ThreadType.PrivateGroup)) {
            String myRole = roleForUser(thread, ChatSDK.currentUser());
            String role = roleForUser(thread, user);
            return Permission.isOr(myRole, Permission.Owner, Permission.Admin) && Permission.isOr(role, Permission.Member, Permission.Watcher, Permission.Banned);
        }
        return false;
    }
}
