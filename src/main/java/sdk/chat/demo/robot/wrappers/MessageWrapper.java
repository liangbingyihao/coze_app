package sdk.chat.demo.robot.wrappers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.ReadReceiptUserLink;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.hook.HookEvent;
import sdk.chat.core.interfaces.ThreadType;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.core.types.ReadStatus;
import sdk.chat.firebase.adapter.FirebasePaths;
import sdk.chat.firebase.adapter.utils.Generic;
import sdk.guru.common.RX;
import sdk.guru.realtime.RXRealtime;

public class MessageWrapper  {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    protected Message model;

    public MessageWrapper(Message model) {
        this.model = model;
    }

    public MessageWrapper(DataSnapshot snapshot) {
        this.model = ChatSDK.db().fetchOrCreateEntityWithEntityID(Message.class, snapshot.getKey());
        deserialize(snapshot);
    }

    public MessageWrapper(String entityID) {
        this.model = ChatSDK.db().fetchOrCreateEntityWithEntityID(Message.class, entityID);
    }

    protected Map<String, Object> serialize() {
        Map<String, Object> values = new HashMap<>();

        Map<String, String> meta = null;

        // We are going to encrypt the message here
        if (ChatSDK.encryption() != null) {
            meta = ChatSDK.encryption().encrypt(model);
        }
        if (meta == null) {
            meta = model.getMetaValuesAsMap();
        }

        values.put(Keys.Meta, meta);
        values.put(Keys.Date, ServerValue.TIMESTAMP);
        values.put(Keys.Type, model.getType());

        HashMap<String, Map<String, Integer>> map = new HashMap<>();
        for (ReadReceiptUserLink link: getModel().getReadReceiptLinks()) {
            HashMap<String, Integer> status = new HashMap<>();
            status.put(Keys.Status, link.getStatus());
            map.put(link.getUser().getEntityID(), status);
        }
        if (!map.isEmpty()) {
            values.put(FirebasePaths.ReadPath, map);
        }
        if (model.getThread().typeIs(ThreadType.Private)) {
            values.put(Keys.To, getTo());
        }

        values.put(Keys.From, model.getSender().getEntityID());

        return values;
    }

    protected ArrayList<String> getTo() {
        ArrayList<String> users = new ArrayList<>();
        for (User user : model.getThread().getUsers()) {
            if(!user.isMe()) {
                users.add(user.getEntityID());
            }
        }
        return users;
    }

    protected boolean contains(Map<String, Object> value, String key) {
        return value.containsKey(key) && !value.get(key).equals("");
    }

    protected void deserialize(DataSnapshot snapshot) {

        if (snapshot.getValue() == null) {
            return;
        }

        if (snapshot.hasChild(Keys.Meta)) {
            // If this has an encrypted payload decrypt it
            Map<String, Object> meta = null;
            if (ChatSDK.encryption() != null) {
                String data = snapshot.child(Keys.Meta).child(Keys.MessageEncryptedPayloadKey).getValue(String.class);
                if (data != null) {
                    try {
                        meta = ChatSDK.encryption().decrypt(data);
                    } catch (Exception e) {
                        model.setEncryptedText(data);
                    }
                }
            }
            if (meta == null) {
//                meta = snapshot.child(Keys.Meta).getValue(Generic.mapStringObject());
                meta = snapshot.child(Keys.Meta).getValue(Generic.mapStringObject());
            }

            model.setMetaValues(meta);

        } else {
            Logger.debug("Message has no meta");
//            model.setText("");
        }

        if (snapshot.hasChild(Keys.Type)) {
            model.setType(snapshot.child(Keys.Type).getValue(Long.class).intValue());
        }

        if (snapshot.hasChild(Keys.Date)) {
            Long date = snapshot.child(Keys.Date).getValue(Long.class);
            if (date != null) {
                model.setDate(new Date(date));
                ChatSDK.events().source().accept(NetworkEvent.messageDateUpdated(model));
            }
        } else {
            Logger.debug("No Date");
        }

        if (snapshot.hasChild(Keys.From)) {
            String senderID = snapshot.child(Keys.From).getValue(String.class);
            User user = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, senderID);
            if (user.getName() == null || user.getName().isEmpty()) {
                ChatSDK.core().userOn(user).subscribe(ChatSDK.events());
            }
            model.setSender(user);
        }

        Map<String, Map<String, Long>> readMap = snapshot.child(Keys.Read).getValue(Generic.readReceiptHashMap());
        if (readMap != null) {

//            Map<String, Long> userMap = readMap.get(ChatSDK.currentUserID());
//            if (userMap != null && userMap.get(Keys.Status) == ReadStatus.Read) {
//                Logger.debug("Message Read: " + model.getEntityID());
//                model.setIsRead(true);
//            }

            updateReadReceipts(readMap).doOnSuccess(aBoolean -> {
                if (aBoolean) {
                    ChatSDK.events().source().accept(NetworkEvent.messageReadReceiptUpdated(model));
                }
            }).ignoreElement().subscribe(ChatSDK.events());
        }

        ChatSDK.db().update(model);
    }

    public Single<Boolean> updateReadReceipts(Map<String, Map<String, Long>> map) {
        return Single.defer((Callable<SingleSource<Boolean>>) () -> {

            final List<Single<Boolean>> singles = new ArrayList<>();

            for(String key : map.keySet()) {

                User user = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, key);

                Map<String, Long> statusMap = map.get(key);

                if (statusMap != null) {

                    Long status = statusMap.get(Keys.Status);
                    if (status == null) {
                        status = (long) ReadStatus.None;
                    }

                    Long date = statusMap.get(Keys.Date);
                    if (date == null) {
                        date = 0L;
                    }

                    singles.add(model.setUserReadStatusAsync(user, new ReadStatus(status.intValue()), new Date(date), false));

                }
            }

            final List<Boolean> outcomes = new ArrayList<>();
            return Single.merge(singles).doOnNext(outcomes::add).ignoreElements().toSingle((Callable<Boolean>) () -> {
                for (Boolean b: outcomes) {
                    if (b) {
                        return true;
                    }
                }
                return false;
            });
        }).subscribeOn(RX.computation());
    }

    public Completable push() {
        return Completable.create(e -> {

            final DatabaseReference ref = ref();
            model.setEntityID(ref.getKey());
            ChatSDK.db().update(model);
            askRobot(e);

        }).subscribeOn(RX.io());
    }

    protected void askRobot(CompletableEmitter e){
        Type typeObject = new TypeToken<HashMap>() {}.getType();
        String gsonData = gson.toJson(model.getMetaValuesAsMap(), typeObject);

        RequestBody body = RequestBody.create(
                gsonData,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://8.217.172.116:5000/api/chat")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                System.err.println("请求失败: " + error.getMessage());
                e.onError(new Exception(ChatSDK.getString(sdk.chat.firebase.adapter.R.string.message_failed_to_send)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> map = gson.fromJson(responseBody, type);
//                ChatSDK.db().update(model);

                //
                Message resp = createMessageResp("hahaha");

                boolean newMessage = resp.getMessageStatus() == MessageSendStatus.None;

                ChatSDK.hook().executeHook(HookEvent.MessageReceived, new HashMap<String, Object>() {{
                    put(HookEvent.Message,resp);
                    put(HookEvent.Thread, model.getThread());
                    put(HookEvent.IsNew_Boolean, newMessage);
                }}).doOnComplete(() -> {
//                        message.markAsReceived().subscribe(ChatSDK.events());
                    model.getThread().addMessage(resp, newMessage);
                }).subscribe(ChatSDK.events());

                e.onComplete();
            }
        });
    }


    protected Message createMessageResp(String content) {

        Message message = ChatSDK.thread().newMessage(MessageType.Text, model.getThread(), true);
        message.setText(content);
        List<User> users = model.getThread().getUsers();
        message.setSender(users.get(0));
//        if (messageDidCreateUpdateAction != null) {
//            messageDidCreateUpdateAction.update(message);
//        }
        ChatSDK.db().update(message, false);
        ChatSDK.events().source().accept(NetworkEvent.messageAdded(message));
        if (message.getPreviousMessage() != null) {
            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message.getPreviousMessage()));
        }
        return message;
    }

    public Completable send() {
        return Completable.defer(() -> {
            if (model.getThread() != null) {
                return push();
            } else {
                return Completable.error(ChatSDK.getException(sdk.chat.firebase.adapter.R.string.message_doesnt_have_a_thread));
            }
        }).subscribeOn(RX.io());
    }

    public Completable delete() {
        return Completable.create(e -> {
            DatabaseReference ref = FirebasePaths.threadMessagesRef(model.getThread().getEntityID()).child(model.getEntityID());
            ref.removeValue((databaseError, databaseReference) -> {
                if (databaseError == null) {
                    e.onComplete();
                }
                else {
                    e.onError(new Throwable(databaseError.getMessage()));
                }
            });
        }).subscribeOn(RX.io());
    }

    protected DatabaseReference ref() {
        if (model.getEntityID() != null && !model.getEntityID().isEmpty()) {
            return FirebasePaths.threadMessagesRef(model.getThread().getEntityID()).child(model.getEntityID());
        }
        else {
            return FirebasePaths.threadMessagesRef(model.getThread().getEntityID()).push();
        }
    }

    public Message getModel() {
        return model;
    }

    public Completable markAsReceived() {
        return setReadStatus(ReadStatus.delivered());
    }

    public Completable setReadStatus (ReadStatus status) {
        return Completable.defer(() -> {
            if (model.getSender().isMe() || model.getThread().typeIs(ThreadType.Public) || !ChatSDK.thread().hasVoice(model.getThread(), ChatSDK.currentUser())) {
                return Completable.complete();
            }

            User currentUser = ChatSDK.currentUser();

            if (!model.getThread().containsUser(currentUser)) {
                return Completable.complete();
            }

            return model.setUserReadStatusAsync(ChatSDK.currentUser(), status, new Date(), true).flatMapCompletable(aBoolean -> {
                if (aBoolean) {
                    Map<String, Object> map = new HashMap<String, Object>() {{
                        put(Keys.Status, status.getValue());
                        put(Keys.Date, ServerValue.TIMESTAMP);
                    }};

                    DatabaseReference ref = FirebasePaths.threadMessagesReadRef(model.getThread().getEntityID(), model.getEntityID()).child(currentUser.getEntityID());
                    RXRealtime realtime = new RXRealtime();
                    Completable completable = realtime.set(ref, map);

                    // TODO: Is this needed?
//                    realtime.addToReferenceManager();

                    return completable;

                }
                return Completable.complete();
            });

            // Do this to stop duplication of links
        }).subscribeOn(RX.single());
    }
}
