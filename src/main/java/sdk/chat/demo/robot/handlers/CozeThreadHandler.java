package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.core.types.Progress;
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

    public Completable removeUsersFromThread(final Thread thread, List<User> users) {
        return Completable.complete();
    }

    public Completable pushThread(Thread thread) {
        return Completable.complete();
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
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    ChatSDK.db().update(message);
                    pushForMessage(message);
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    return Single.just(message);
                }).ignoreElement();

    }

    public Single<Thread> createRobotThread() {
        Thread t = ChatSDK.db().fetchThreadWithEntityID("-1");
        if (t != null) {
            return Single.just(t);
        }
        Thread thread = ChatSDK.db().createEntity(Thread.class);
        thread.setEntityID("-1");
        thread.setCreator(ChatSDK.currentUser());
        thread.setCreationDate(new Date());
        thread.setType(ThreadType.Private1to1);
        ChatSDK.db().update(thread);
        return Single.just(thread);
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

                    ArrayList<User> users = new ArrayList<>();
                    if(theUsers!=null&&!theUsers.isEmpty()){
                        users.addAll(theUsers);
                    }
                    User currentUser = ChatSDK.currentUser();
                    users.add(currentUser);
                    thread.addUsers(users);
                    thread.setType(ThreadType.Private1to1);
                    ChatSDK.db().update(thread);
                    return Single.just(thread);
                });
    }

    public Completable deleteThread(Thread thread) {
        //FIXME
        return Completable.complete();
    }

    protected void pushForMessage(final Message message) {
        if (ChatSDK.push() != null && message.getThread().typeIs(ThreadType.Private)) {
            Map<String, Object> data = ChatSDK.push().pushDataForMessage(message);
            ChatSDK.push().sendPushNotification(data);
        }
    }

    public Completable deleteMessage(Message message) {
        //FIXME
        return Completable.complete();
    }

    public Completable leaveThread(Thread thread) {
        //FIXME
        return Completable.complete();
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
    public String localizeRole(String role) {
        return Permission.toLocalized(role);
    }


    @Override
    public String generateNewMessageID(Thread thread) {
        // User Firebase to generate an ID
        return FirebasePaths.threadMessagesRef(thread.getEntityID()).push().getKey();
    }

    @Override
    public boolean canDestroy(Thread thread) {
        return false;
    }

    @Override
    public Completable destroy(Thread thread) {
        return Completable.complete();
    }


    public Single<List<Thread>> listOrNewSessions() {
        return listSessions(ThreadType.Private1to1)
                .flatMap(sessions -> {
                    if (!sessions.isEmpty()) {
                        return Single.just(sessions);
                    }
                    // 如果不存在会话，则创建新会话后再查询
                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
                            .flatMap(ignored -> listSessions(ThreadType.Private1to1)) ;
                })
                .onErrorResumeNext(error -> {
                    // 错误处理逻辑
                    if (error instanceof IOException) {
                        return Single.error(new IOException("Failed to list sessions", error));
                    }
                    return Single.error(error);
                });
    }

    private Single<List<Thread>> listSessions(int type) {
        return Single.fromCallable(() -> {
            try {
                List<Thread> data = ChatSDK.db().fetchThreadsWithType(type);
                Collections.sort(data, (a, b) -> {
                    return Long.compare(b.getCreationDate().getTime(), a.getCreationDate().getTime()); // 倒序
                });
                return data;
            } catch (Exception e) {
                throw new IOException("Failed to get threads", e);
            }
        }).subscribeOn(RX.io());
    }

    public Single<List<Thread>> newAndListSessions() {
        return listSessions(ThreadType.Private1to1)
                .flatMap(sessions -> {
                    if (!sessions.isEmpty() && sessions.get(0).getMessages().isEmpty()) {
                        return Single.just(sessions);
                    }
                    // 如果不存在会话，则创建新会话后再查询
                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
                            .flatMap(ignored -> listSessions(ThreadType.Private1to1)) ;
                })
                .onErrorResumeNext(error -> {
                    // 错误处理逻辑
                    if (error instanceof IOException) {
                        return Single.error(new IOException("Failed to list sessions", error));
                    }
                    return Single.error(error);
                });
    }

}
