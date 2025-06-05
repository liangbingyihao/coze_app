package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.greenrobot.greendao.query.QueryBuilder;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import sdk.chat.core.base.AbstractThreadHandler;
import sdk.chat.core.dao.CachedFile;
import sdk.chat.core.dao.DaoCore;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.MessageDao;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.ThreadDao;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.rigs.MessageSendRig;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.BuildConfig;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.GWApiManager;
import sdk.chat.demo.robot.api.model.ImageTagList;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.api.model.SystemConf;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;
import sdk.guru.common.RX;

public class GWThreadHandler extends AbstractThreadHandler {
    private final AtomicBoolean hasSyncedWithNetwork = new AtomicBoolean(false);
    private List<Thread> sessionCache;
    private Message welcome;
    private AIExplore aiExplore;
    private Boolean isCustomPrompt = null;
    private SystemConf serverPrompt = null;
    private final static Gson gson = new Gson();

    public AIExplore getAiExplore() {
        return aiExplore;
    }

    public Single<SystemConf> getServerPrompt() {
        if (BuildConfig.DEBUG) {
            Log.d("BuildMode", "当前是 Debug 包");

            if (serverPrompt != null) {
                return Single.just(serverPrompt);
            }

            return GWApiManager.shared().getConf()
                    .subscribeOn(RX.io())
                    .flatMap(conf -> {
                        if (conf == null) {
                            return Single.error(new NullPointerException("Configuration is null"));
                        }
                        serverPrompt = conf;
                        return Single.just(serverPrompt);
                    }).subscribeOn(RX.db());
        } else {
            return Single.just(serverPrompt);
        }
    }

    public boolean isCustomPrompt() {
        if (isCustomPrompt == null) {
            try {
                isCustomPrompt = MainApp.getContext().getSharedPreferences(
                        "ai_prompt",
                        Context.MODE_PRIVATE // 仅当前应用可访问
                ).getBoolean("isCustomPrompt", false);
            } catch (Exception e) {
                isCustomPrompt = false;
            }
        }
        return isCustomPrompt;
    }

    public void setCustomPrompt(boolean customPrompt) {
        MainApp.getContext().getSharedPreferences(
                "ai_prompt",
                Context.MODE_PRIVATE // 仅当前应用可访问
        ).edit().putBoolean("isCustomPrompt", customPrompt).apply();
        isCustomPrompt = customPrompt;
    }

    private List<Message> fetchMessagesEarlier(Long startId) {
        // Logger.debug(java.lang.Thread.currentThread().getName());
        DaoCore daoCore = ChatSDK.db().getDaoCore();


        QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);

        if (startId > 0) {
            qb.where(MessageDao.Properties.Id.lt(startId));
        }

        qb.orderDesc(MessageDao.Properties.Date).limit(20);


        return qb.list();
    }

    public Single<List<Message>> loadMessagesBySession(@Nullable String sessionId) {
        return Single.defer(() -> {
            DaoCore daoCore = ChatSDK.db().getDaoCore();
            QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
            if (sessionId != null) {
                qb.where(MessageDao.Properties.ThreadId.eq(Long.parseLong(sessionId)));
            }
            qb.orderDesc(MessageDao.Properties.Date);
            List<Message> data = qb.list();
            return Single.just(data);
        }).subscribeOn(RX.db());
    }

    public Single<Boolean> setSummary(String msgId, String summary) {
        if (summary == null || summary.isEmpty() || summary.length() > 8) {
            return Single.just(Boolean.FALSE);
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setSummary(msgId, summary)
                .subscribeOn(RX.io())
                .flatMap(isSuccess -> {
                    if (!isSuccess) {
                        return Single.just(false);
                    }
                    return Single.fromCallable(() -> {

                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            message.setMetaValue("summary", summary);
                            ChatSDK.db().update(message, false);
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                            return true;
                        }
                        return false;
                    }).subscribeOn(RX.db()).map(updateResult -> true);
                }).onErrorReturnItem(false);
    }

    public Single<Long> setSession(String msgId, Long sessionId) {
        if (sessionId == null) {
            return Single.error(new IllegalArgumentException("SessionId ID cannot be null or empty"));
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setSession(msgId, sessionId, null)
                .subscribeOn(RX.io())
                .flatMap(newSessionId -> {
                    if (newSessionId == null) {
                        return Single.just(0L);
                    }
                    return Single.fromCallable(() -> {

                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            message.setThreadId(newSessionId);
                            ChatSDK.db().update(message, false);
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                            return newSessionId;
                        }
                        return 0L;
                    }).subscribeOn(RX.db()).map(updateResult -> updateResult);
                }).onErrorReturnItem(0L);
    }

    public Single<Long> newSession(String msgId, String sessionName) {
        if (sessionName == null) {
            return Single.error(new IllegalArgumentException("SessionId ID cannot be null or empty"));
        }
        if (msgId == null || msgId.isEmpty()) {
            return Single.error(new IllegalArgumentException("Message ID cannot be null or empty"));
        }

        return GWApiManager.shared().setSession(msgId, 0L, sessionName)
                .subscribeOn(RX.io())
                .flatMap(newSessionId -> {
                    if (newSessionId == null) {
                        return Single.just(0L);
                    }
                    return Single.fromCallable(() -> {
                        if (newSessionId > 0) {
                            updateThread(newSessionId.toString(), sessionName, new Date());
                        }
                        Message message = ChatSDK.db().fetchMessageWithEntityID(msgId);
                        if (message != null) {
                            message.setThreadId(newSessionId);
                            ChatSDK.db().update(message, false);
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                            return newSessionId;
                        }
                        return 0L;
                    }).subscribeOn(RX.db()).map(updateResult -> updateResult);
                }).onErrorReturnItem(0L);
    }

    public Single<List<Message>> loadMessagesEarlier(@Nullable Long startId, boolean loadFromServer) {
        return Single.defer(() -> {
            List<Message> messages = fetchMessagesEarlier(startId);
            int i = 0;
            while (aiExplore == null && i < messages.size()) {
                Message tmp = messages.get(i);
                if (tmp.stringForKey("explore_0") != null) {
                    aiExplore = AIExplore.loads(tmp);
                }
                ++i;
            }
            return Single.just(messages);
        }).subscribeOn(RX.db());
    }


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

    public Completable sendExploreMessage(final String text, final String contextId, final Thread thread, int action, String params) {
        return new MessageSendRig(new MessageType(MessageType.Text), thread, message -> {
            message.setText(text);
            message.setMetaValue("context_id", contextId);
            message.setMetaValue("action", action);
//    action_daily_ai = 0
//    action_bible_pic = 1
//    action_daily_gw = 2
//    action_direct_msg = 3
//    action_daily_pray = 4
            if (action == 3) {
                message.setMetaValue("feedback", params);
            } else if (action == 1) {
                message.setType(MessageType.Image);
            }
        }).run();
    }

    /**
     * Send a text,
     * The text need to have a owner thread attached to it or it cant be added.
     * If the destination thread is public the system will add the user to the text thread if needed.
     * The uploading to the server part can bee seen her {@see FirebaseCoreAdapter#PushMessageWithComplition}.
     */
    public Completable sendMessage(final Message message) {
        String action = message.stringForKey("action");
        if ("3".equals(action) || "1".equals(action)) {
            return Completable.complete();
        }
        String prompt = isCustomPrompt ? MainApp.getContext().getSharedPreferences(
                "ai_prompt",
                Context.MODE_PRIVATE // 仅当前应用可访问
        ).getString("ai_prompt" + action, null) : null;
        return GWApiManager.shared().askRobot(message, prompt)
                .subscribeOn(RX.io()).flatMap(data -> {
                    message.setEntityID(data.get("id").getAsString());
                    ChatSDK.db().update(message);
                    startPolling(data.get("id").getAsString());
//                    message.setMessageStatus(MessageSendStatus.Uploading,true);
//                    ChatSDK.events().source().accept(NetworkEvent.messageProgressUpdated(message, new Progress(10,100)));
                    pushForMessage(message);
//                    ChatSDK.events().source().accept(NetworkEvent.threadMessagesUpdated(message.getThread()));
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

        return GWApiManager.shared().saveSession("1")
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
//        if (ChatSDK.push() != null && message.getThread().typeIs(ThreadType.Private)) {
//            Map<String, Object> data = ChatSDK.push().pushDataForMessage(message);
//            ChatSDK.push().sendPushNotification(data);
//        }
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
        //                    if (!sessions.isEmpty()) {
        //                        return Single.just(sessions);
        //                    }
        //                    // 如果不存在会话，则创建新会话后再查询
        //                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
        //                            .flatMap(ignored -> listSessions());
        return listSessions()
                .flatMap(Single::just)
                .onErrorResumeNext(error -> {
                    // 错误处理逻辑
                    if (error instanceof IOException) {
                        return Single.error(new IOException("Failed to list sessions", error));
                    }
                    return Single.error(error);
                });
    }

    public String getSessionName(Long sessionId) {
        if (sessionId != null && sessionId > 0 && sessionCache != null && !sessionCache.isEmpty()) {
            String strId = sessionId.toString();
            for (Thread session : sessionCache) {
                if (session.getEntityID().equals(strId)) {
                    return session.getName();
                }

            }
        }
        return null;
    }

    public Single<List<Thread>> listSessions() {
        // 1. 检查内存缓存
        if (sessionCache != null && !sessionCache.isEmpty()) {
            return Single.just(sessionCache);
        }
        return Single.fromCallable(() -> {
            try {
                DaoCore daoCore = ChatSDK.db().getDaoCore();
                QueryBuilder<Thread> qb = daoCore.getDaoSession().queryBuilder(Thread.class);
                qb.where(ThreadDao.Properties.Type.eq(ThreadType.None));
                qb.orderDesc(ThreadDao.Properties.LastMessageDate);
                List<Thread> data = qb.list();

//                List<Thread> data = ChatSDK.db().fetchThreadsWithType(ThreadType.None);
//                Collections.sort(data, (a, b) -> {
//                    return Long.compare(b.getEntityID(), a.getCreationDate().getTime()); // 倒序
//                });

                if (!hasSyncedWithNetwork.get()) {
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
        //                    if (!sessions.isEmpty() && sessions.get(0).getMessages().isEmpty()) {
        //                        return Single.just(sessions);
        //                    }
        //                    // 如果不存在会话，则创建新会话后再查询
        //                    return createThread("新会话", ChatSDK.contact().contacts(), ThreadType.Private1to1)
        //                            .flatMap(ignored -> {
        //                                sessionCache = null;
        //                                return listSessions();
        //                            });
        return listSessions()
                .flatMap(Single::just)
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
        GWApiManager.shared().listSession(1, 50)
                .subscribeOn(RX.io())
//                .observeOn(RX.io())
                .subscribe(
                        networkSessions -> {
                            boolean modified = false;
                            if (networkSessions != null) {
                                JsonArray items = networkSessions.getAsJsonObject().getAsJsonArray("items");
                                if (!items.isEmpty()) {
                                    for (JsonElement i : items) {
                                        JsonObject session = i.getAsJsonObject();
                                        String sessionName = session.get("session_name").getAsString();
                                        String entityId = session.get("id").getAsString();
                                        Date updateAt = DateLocalizationUtil.INSTANCE.toDate(session.get("updated_at").getAsString());
                                        if (updateThread(entityId, sessionName, updateAt)) {
                                            modified = true;
                                        }
                                    }
                                }
                            }
                            hasSyncedWithNetwork.set(true);

                            // 更新内存缓存
                            if (modified) {
                                sessionCache = null;
                                ChatSDK.events().source().accept(NetworkEvent.threadsUpdated());
                            }
                        },
                        error -> {
                        }
                );
    }

    public Thread createChatSessions() {
        DaoCore daoCore = ChatSDK.db().getDaoCore();
        QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
        qb.where(MessageDao.Properties.Type.eq(MessageType.Audio));
        List<Message> data = qb.list();
        for (Message d : data) {
            daoCore.deleteEntity(d);
        }

        String entityID = "0";
        Thread thread = ChatSDK.db().fetchThreadWithEntityID(entityID);
        if (thread != null) {
            return thread;
        }
        thread = ChatSDK.db().createEntity(Thread.class);
        thread.setEntityID("0");
        thread.setCreator(ChatSDK.currentUser());
        thread.setCreationDate(new Date());
        thread.setName("chat", false);
        ArrayList<User> users = new ArrayList<>(ChatSDK.contact().contacts());
        User currentUser = ChatSDK.currentUser();
        users.add(currentUser);
        thread.addUsers(users);
        thread.setType(ThreadType.Private1to1);
        ChatSDK.db().update(thread);
        return thread;
    }

    public boolean updateThread(String threadId, String sessionName, Date updateAt) {
        Thread entity = ChatSDK.db().fetchOrCreateThreadWithEntityID(threadId);
        boolean modified = false;
        if (!sessionName.equals(entity.getName())) {
            entity.setName(sessionName);
            entity.setType(ThreadType.None);
            modified = true;
        }
        if (updateAt != null && (entity.getLastMessageDate() == null || entity.getLastMessageDate().before(updateAt))) {
            entity.setLastMessageDate(updateAt);
            modified = true;
        }
        if (modified) {
            ChatSDK.db().update(entity);
            sessionCache = null;
            ChatSDK.events().source().accept(NetworkEvent.threadsUpdated());
        }
        return modified;
    }


    public Message newMessage(int type, Thread thread, boolean notify) {
        Message message = new Message();
        message.setSender(ChatSDK.currentUser());
        message.setDate(new Date());

        message.setEntityID(generateNewMessageID(thread));
        message.setType(type);
        message.setMessageStatus(MessageSendStatus.Initial, false);
        message.setIsRead(true);

        ChatSDK.db().insertOrReplaceEntity(message);

        return message;
    }

    @SuppressLint("CheckResult")
    public Single<Message> getWelcomeMsg() {
        if (welcome != null) {
            return Single.just(welcome);
        }
        return Single.fromCallable(() -> {
            try {
                DaoCore daoCore = ChatSDK.db().getDaoCore();
                QueryBuilder<Message> qb = daoCore.getDaoSession().queryBuilder(Message.class);
                qb.where(MessageDao.Properties.EntityID.eq("welcome")).limit(1);
                List<Message> data = qb.list();

                if (data.isEmpty()) {
                    GWApiManager.shared().getMessageDetail("welcome")
                            .subscribeOn(RX.io())
                            .subscribe(
                                    json -> {
                                        Message message = new Message();
                                        message.setEntityID("welcome");
                                        message.setSender(ChatSDK.currentUser());
                                        message.setDate(new Date());
                                        message.setType(MessageType.Text);
                                        message.setMessageStatus(MessageSendStatus.Initial, false);
                                        ChatSDK.db().insertOrReplaceEntity(message);
                                        ChatSDK.events().source().accept(NetworkEvent.messageAdded(message));
                                        updateMessage(message, json);
                                    },
                                    error -> {
                                    }
                            );
                }
                welcome = data.get(0);
                return welcome;
            } catch (Exception e) {
                throw new IOException("Failed to get threads", e);
            }
        }).subscribeOn(RX.io());
//        return listSessions()
//                .flatMap(Single::just)
//                .onErrorResumeNext(error -> {
//                    // 错误处理逻辑
//                    if (error instanceof IOException) {
//                        return Single.error(new IOException("Failed to list sessions", error));
//                    }
//                    return Single.error(error);
//                });
    }

    private void updateMessage(Message message, JsonObject json) {
        MessageDetail messageDetail = gson.fromJson(json, MessageDetail.class);
        int status = json.get("status").getAsInt();
        String feedbackText = json.get("feedback_text").getAsString();
        long sessionId = 0;
        String sessionName = "";
        if (status == 2) {
            try {
                disposables.clear();
                sessionId = json.get("session_id").getAsLong();
                if (sessionId > 0) {
                    message.setThreadId(sessionId);
                }
                JsonElement summary = json.get("summary");
                try {
                    if (summary != null) {
                        message.setMetaValue("summary", summary.getAsString());
                    }
                } catch (UnsupportedOperationException e) {
                    // 备选方案：使用JSON字符串形式
                    message.setMetaValue("summary", summary.toString());
                }
                JsonObject feedback = json.getAsJsonObject("feedback");
                if (feedback != null) {
                    JsonArray exploreArray = feedback.getAsJsonArray("function");
                    for (int i = 0; i < exploreArray.size(); i++) {
                        String function = exploreArray.get(i).getAsJsonArray().toString();
                        message.setMetaValue("explore_" + i, function);
                    }
                    if (feedback.has("topic")) {
                        sessionName = feedback.get("topic").getAsString();
                    }
                    if (feedback.has("color_tag")) {
                        String colorTag = feedback.get("color_tag").getAsString();
                        if (colorTag != null) {
                            message.setMetaValue("colorTag", colorTag);
                        }
                    }
                    if (feedback.has("tag")) {
                        String tag = feedback.get("tag").getAsString();
                        if (tag != null) {
                            message.setMetaValue("tag", tag);
                        }
                    }
                    if (feedback.has("bible")) {
                        String bible = feedback.get("bible").getAsString();
                        if (bible != null) {
                            message.setMetaValue("bible", bible);
                        }
                    }

                    AIExplore newAIExplore = AIExplore.loads(message);
                    if (newAIExplore != null) {
                        Message oldMsg = aiExplore != null ? aiExplore.getMessage() : null;
                        aiExplore = newAIExplore;
                        if (oldMsg != null) {
                            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(oldMsg));
                        }
                    }
                }
            } catch (Exception e) {
            }
        } else if (status == 1) {
            feedbackText = "(生成中)\n" + feedbackText;
        }
        message.setMetaValue("feedback", feedbackText);

        ChatSDK.db().update(message, false);
        if (sessionId > 0) {
            updateThread(Long.toString(sessionId), sessionName, new Date());
        }
        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));

    }


    private final CompositeDisposable disposables = new CompositeDisposable();
    private final AtomicInteger retryCount = new AtomicInteger(0);    // 配置参数
    private volatile boolean isPolling = false;
    private static final long INITIAL_DELAY = 0; // 立即开始
    private static final long POLL_INTERVAL = 3; // 5秒间隔
    private static final long REQUEST_TIMEOUT = 15; // 单个请求10秒超时
    private static final long OPERATION_TIMEOUT = 2; // 整体操作2分钟超时
    private static final int MAX_RETRIES = 3; // 最大重试次数

    public synchronized boolean isPolling() {
        return isPolling;
    }

    public synchronized void stopPolling() {
        disposables.clear();
        isPolling = false;
    }

    public synchronized void startPolling(String contextId) {
        if (isPolling) {
            Logger.warn("轮询已在运行中");
            return;
        }

        isPolling = true;
        Disposable disposable = Observable.interval(INITIAL_DELAY, POLL_INTERVAL, TimeUnit.SECONDS)
                .doOnDispose(() -> {
                            isPolling = false;
                        }
                )
                .flatMap(tick -> {
                    retryCount.set(0);
                    return GWApiManager.shared().getMessageDetail(contextId)
                            .subscribeOn(RX.io())
                            .flatMap(Single::just).toObservable();
                })
                .observeOn(RX.db())
                .flatMapCompletable(json ->
                        {
                            if (json != null) {
                                Message message = ChatSDK.db().fetchMessageWithEntityID(contextId);
                                updateMessage(message, json);
                                return Completable.complete();
                            } else {
                                return Completable.error(new Throwable());
                            }
                        }
                )
                .timeout(OPERATION_TIMEOUT, TimeUnit.MINUTES)
                .observeOn(RX.main())
                .subscribe(() -> {
                            Logger.warn("success...");
                        },
                        error -> Logger.warn("failed..." + error.getMessage()));

        disposables.add(disposable);
    }
}
