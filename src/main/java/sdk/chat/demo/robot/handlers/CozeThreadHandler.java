package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.Single;
import sdk.chat.core.base.AbstractThreadHandler;
import sdk.chat.core.dao.CachedFile;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.rigs.MessageSendRig;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.robot.api.CozeApiManager;
import sdk.guru.common.RX;

/**
 * Created by benjaminsmiley-andrews on 25/05/2017.
 */

public class CozeThreadHandler extends AbstractThreadHandler {
    private final AtomicBoolean hasSyncedWithNetwork = new AtomicBoolean(false);
    private List<Thread> sessionCache;

    @Override
    public Single<List<Message>> loadMoreMessagesBefore(Thread thread, @Nullable Date before) {
        return super.loadMoreMessagesBefore(thread, before);
    }

    @Override
    public Single<List<Message>> loadMoreMessagesBefore(Thread thread, @Nullable Date before, boolean loadFromServer) {
        return super.loadMoreMessagesBefore(thread, before, loadFromServer);
    }

    @Override
    public Single<List<Message>> loadMoreMessagesAfter(Thread thread, @Nullable Date after, boolean loadFromServer) {
//        return super.loadMoreMessagesAfter(thread, after, loadFromServer);

        return super.loadMoreMessagesAfter(thread, after, loadFromServer).flatMap(localMessages -> {
            ArrayList<Message> mergedMessages = new ArrayList<>(localMessages);
            return Single.just(mergedMessages);
        });
    }

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

    public Completable sendExploreMessage(final String text,final String contextId, final Thread thread) {
        return new MessageSendRig(new MessageType(MessageType.Text), thread, message -> {
            message.setText(text);
            message.setMetaValue("contextId",contextId);
        }).run();
    }

    /**
     * Send a text,
     * The text need to have a owner thread attached to it or it cant be added.
     * If the destination thread is public the system will add the user to the text thread if needed.
     * The uploading to the server part can bee seen her {@see FirebaseCoreAdapter#PushMessageWithComplition}.
     */
    public Completable sendMessage(final Message message) {
        return CozeApiManager.shared().askRobot(message)
                .subscribeOn(RX.io()).flatMap(data -> {
                    message.setEntityID(data.get("id").getAsString());
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    ChatSDK.db().update(message);
                    pushForMessage(message);
                    ChatSDK.events().source().accept(NetworkEvent.threadMessagesUpdated(message.getThread()));
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    return Single.just(message);
                }).ignoreElement();

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
                    if (theUsers != null && !theUsers.isEmpty()) {
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
        return Completable.defer(() -> {
            if (message.getSender().isMe() && message.getMessageStatus().equals(MessageSendStatus.Sent) && !message.getMessageType().is(MessageType.System)) {
                // If possible delete the files associated with this message

                List<CachedFile> files = ChatSDK.db().fetchFilesWithIdentifier(message.getEntityID());
                for (CachedFile file: files) {
                    if (file.getRemotePath() != null) {
                        ChatSDK.upload().deleteFile(file.getRemotePath()).subscribe();
                    }
                    ChatSDK.db().delete(file);
                }

                // TODO: 删除后端...
//                MessagePayload payload = ChatSDK.getMessagePayload(message);
//                if (payload != null) {
//                    List<String> paths = payload.remoteURLs();
//                    for (String path: paths) {
//                        ChatSDK.upload().deleteFile(path).subscribe();
//                    }
//                }

//                return FirebaseModule.config().provider.messageWrapper(message).delete();
            }
            message.getThread().removeMessage(message);
            return Completable.complete();
        });
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
        return "";
    }


    @Override
    public String generateNewMessageID(Thread thread) {
        // User Firebase to generate an ID
        return Long.toString(System.currentTimeMillis());
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
        return listSessions()
                .flatMap(sessions -> {
                    if (!sessions.isEmpty()) {
                        return Single.just(sessions);
                    }
                    // 如果不存在会话，则创建新会话后再查询
                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
                            .flatMap(ignored -> listSessions());
                })
                .onErrorResumeNext(error -> {
                    // 错误处理逻辑
                    if (error instanceof IOException) {
                        return Single.error(new IOException("Failed to list sessions", error));
                    }
                    return Single.error(error);
                });
    }

    private Single<List<Thread>> listSessions() {
        // 1. 检查内存缓存
        if (sessionCache != null && !sessionCache.isEmpty()) {
            return Single.just(sessionCache);
        }
        return Single.fromCallable(() -> {
            try {
                List<Thread> data = ChatSDK.db().fetchThreadsWithType(ThreadType.Private1to1);
                Collections.sort(data, (a, b) -> {
                    return Long.compare(b.getCreationDate().getTime(), a.getCreationDate().getTime()); // 倒序
                });

                if(!hasSyncedWithNetwork.get()){
                    triggerNetworkSync();
                }
                sessionCache = data;
                return data;
            } catch (Exception e) {
                throw new IOException("Failed to get threads", e);
            }
        }).subscribeOn(RX.io());
    }

    public Single<List<Thread>> newAndListSessions() {
        return listSessions()
                .flatMap(sessions -> {
                    if (!sessions.isEmpty() && sessions.get(0).getMessages().isEmpty()) {
                        return Single.just(sessions);
                    }
                    // 如果不存在会话，则创建新会话后再查询
                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
                            .flatMap(ignored -> {
                                sessionCache = null;
                                return listSessions();
                            });
                })
                .onErrorResumeNext(error -> {
                    // 错误处理逻辑
                    if (error instanceof IOException) {
                        return Single.error(new IOException("Failed to list sessions", error));
                    }
                    return Single.error(error);
                });
    }

    @SuppressLint("CheckResult")
    public void triggerNetworkSync() {
        CozeApiManager.shared().listSession(1,50)
                .subscribeOn(RX.io())
                .observeOn(RX.io())
                .subscribe(
                        networkSessions -> {
                            boolean modified = false;
                            if (networkSessions != null) {
                                JsonArray items = networkSessions.getAsJsonObject().getAsJsonArray("items");
                                if (!items.isEmpty()) {
                                    for (JsonElement i : items) {
                                        JsonObject session = i.getAsJsonObject();
                                        Thread entity = ChatSDK.db().fetchThreadWithEntityID(session.get("id").getAsString());
                                        String newName = session.get("session_name").getAsString();
                                        if(entity!=null&&newName!=null&&!newName.equals(entity.getName())){
                                            entity.setName(session.get("session_name").getAsString());
                                            ChatSDK.db().update(entity);
                                            if(!modified){
                                                modified = true;
                                            }
                                        }
                                    }
                                }
                            }
                            hasSyncedWithNetwork.set(true);

                            // 更新内存缓存
                            if(modified){
                                sessionCache = null;
                                ChatSDK.events().source().accept(NetworkEvent.threadsUpdated());
                            }
                        },
                        error -> {
                        }
                );
    }
}
