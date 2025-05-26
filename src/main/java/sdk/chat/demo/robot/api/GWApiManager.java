package sdk.chat.demo.robot.api;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import sdk.chat.core.dao.Message;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sdk.chat.core.dao.User;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.core.types.MessageSendStatus;
import sdk.chat.core.types.MessageType;
import sdk.chat.demo.robot.push.UpdateTokenWorker;
import sdk.guru.common.RX;

import com.google.gson.*;

import org.json.JSONObject;
import org.pmw.tinylog.Logger;

//mysql -h 172.17.0.3 -u root coze_data -p

public class GWApiManager {
    private final Gson gson = new Gson();
    private final OkHttpClient client;
    private String accessToken;
    private final static String URL = "http://8.217.172.116:5000/api/";
    private final static String URL_LOGIN = URL + "auth/login";
    private final static String URL_SESSION = URL + "session";
    private final static String URL_MESSAGE = URL + "message";

    private final static GWApiManager instance = new GWApiManager();

    public static GWApiManager shared() {
        return instance;
    }

    protected GWApiManager() {

    }

    {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new TokenRefreshInterceptor())
                .build();
    }

    public String getAccessToken() {
        return accessToken;
    }

    @SuppressLint("CheckResult")
    public String refreshTokenSync() {
        accessToken = null;
        authenticate(ChatSDK.auth().cachedAccountDetails()).blockingGet();
        return accessToken;
    }

    public Single<AccountDetails> authenticate(final AccountDetails details) {
        return Single.create((SingleOnSubscribe<AccountDetails>) emitter -> {
            Map<String, String> params = new HashMap<>();
            if (details.type == AccountDetails.Type.Username) {
                params.put("username", details.username);
                params.put("password", details.password);
            } else if (details.type == AccountDetails.Type.Custom) {
                params.put("guest", details.token);
            } else {
                emitter.onError(new Exception("login type error"));
            }
            String fcmToken = UpdateTokenWorker.checkAndUpdateToken(ChatSDK.ctx());
            params.put("fcmToken", fcmToken);
            String gsonData = new JSONObject(params).toString();

            RequestBody body = RequestBody.create(
                    gsonData,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_LOGIN)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException error) {
                    System.err.println("请求失败: " + error.getMessage());
                    emitter.onError(error);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    JsonObject resp = gson.fromJson(response.body().string(), JsonObject.class);
                    try {
                        if (resp != null && !resp.get("success").getAsBoolean()) {
                            throw new Exception("login failed:" + resp.get("message").getAsString());
                        }
                        JsonObject data = resp.getAsJsonObject("data");
                        accessToken = "Bearer " + data.get("access_token").getAsString();
                        details.setMetaValue("userId", data.get("user_id").getAsString());
                        emitter.onSuccess(details);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }
            });
        }).subscribeOn(RX.io());
    }

    public Single<JsonObject> saveSession(String robotId) {
        return Single.create(emitter -> {

            Map<String, String> params = new HashMap<>();
            params.put("robot_id", robotId);
            Type typeObject = new TypeToken<HashMap>() {
            }.getType();
            String gsonData = gson.toJson(params, typeObject);
            RequestBody body = RequestBody.create(
                    gsonData,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_SESSION)
//                    .header("Authorization", accessToken)
                    .post(body)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public Single<Boolean> setSummary(String msgId, String summary) {

        return Single.create(emitter -> {
            Map<String, String> params = new HashMap<String, String>();
            params.put("summary", summary);
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE + "/" + msgId)
//                    .header("Authorization", accessToken)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    emitter.onError(new Exception("send msg error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
                        JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
                        try {
                            if (resp != null && !resp.get("success").getAsBoolean()) {
                                throw new Exception("login failed:" + resp.get("message").getAsString());
                            }
                            emitter.onSuccess(Boolean.TRUE);
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });
    }

    public Single<JsonObject> askRobot(Message message) {
        return Single.create(emitter -> {
            Map<String, String> params = message.getMetaValuesAsMap();
//            params.put("session_id", message.getThread().getEntityID());
            RequestBody body = RequestBody.create(
                    new JSONObject(params).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_MESSAGE)
//                    .header("Authorization", accessToken)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException error) {
                    Logger.warn("Message: " + message.getText() + "请求失败: " + error.getMessage());
                    emitter.onError(new Exception("send msg error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            if (response.message().equals("UNAUTHORIZED")) {
                                ChatSDK.auth().logout();
                            }
                            message.setMessageStatus(MessageSendStatus.Failed, true);
                            emitter.onError(new IOException("HTTP error: " + response.code() + "," + responseBody));
                            return;
                        }
//                        ChatSDK.thread().sendLocalSystemMessage("获取回复中...",message.getThread());
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }

                }
            });

        });

    }

    public Single<JsonObject> listSession(int page, int limit) {
        return Single.create(emitter -> {


            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_SESSION))
                    .newBuilder()
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("limit", Integer.toString(limit))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public void logout() {
        accessToken = null;
    }

    public Single<JsonObject> getMessageDetail(String contextId) {
        return Single.create(emitter -> {

            HttpUrl url = HttpUrl.parse(URL_MESSAGE + "/" + contextId)
                    .newBuilder()
                    .build();

            Request request = new Request.Builder()
                    .url(url)
//                    .header("Authorization", accessToken)
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    emitter.onError(e); // 请求失败
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            emitter.onError(new IOException("HTTP error: " + response.code()));
                            return;
                        }
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                        emitter.onSuccess(data); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }


    protected boolean createMessageResp(Message context, JsonObject detail) {
        Message message = ChatSDK.db().fetchMessageWithEntityID(detail.get("id").getAsString());
        int action = detail.get("action").getAsInt();
        if (message == null) {
            message = new Message();
            message.setDate(new Date());
            List<User> users = context.getThread().getUsers();
            message.setSender(users.get(0));
            message.setEntityID(detail.get("id").getAsString());
            if (action != 0) {
                message.setType(MessageType.System);
            } else {
                message.setType(MessageType.Text);
            }
            message.setMessageStatus(MessageSendStatus.Initial, false);
            message.setIsRead(false);
            ChatSDK.db().insertOrReplaceEntity(message);
            context.getThread().addMessage(message, true, true, false);
        }
        message.setText(detail.get("content").getAsString());
        message.setMetaValue("action", detail.get("action").getAsInt());
//        message.setReply("action:"+detail.get("action").getAsString());
        ChatSDK.db().update(message, false);
        ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
//        Message resp = ChatSDK.thread().newMessage(MessageType.Text, context.getThread(), true);
////        if (messageDidCreateUpdateAction != null) {
////            messageDidCreateUpdateAction.update(message);
////        }
//        ChatSDK.db().update(resp, false);
//        context.getThread().addMessage(resp, true);
////        if (ChatSDK.push() != null && resp.getThread().typeIs(ThreadType.Private)) {
////            Map<String, Object> data = ChatSDK.push().pushDataForMessage(resp);
////            ChatSDK.push().sendPushNotification(data);
////        }

//        ChatSDK.events().source().accept(NetworkEvent.messageAdded(resp));
////        if (message.getPreviousMessage() != null) {
////            ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message.getPreviousMessage()));
////        }
//
//        ChatSDK.hook().executeHook(HookEvent.MessageReceived, new HashMap<String, Object>() {{
//            put(HookEvent.Message,resp);
//            put(HookEvent.Thread, context.getThread());
//            put(HookEvent.IsNew_Boolean, true);
//        }}).doOnComplete(() -> {
////                        message.markAsReceived().subscribe(ChatSDK.events());
//            context.getThread().addMessage(resp, true);
//        }).subscribe(ChatSDK.events());
//        return resp;
        return detail.get("status").getAsInt() != 2;

    }

//    // 创建指数退避重试处理器
//    private Function<? super Observable<Throwable>, ? extends ObservableSource<?>> createRetryHandler() {
//        return attempts -> attempts
//                .flatMap((Throwable throwable) -> {
//                    int currentRetry = retryCount.incrementAndGet();
//                    if (currentRetry > MAX_RETRIES) {
//                        Logger.warn("currentRetry > MAX_RETRIES");
//                        return Observable.error(throwable); // 超过最大重试次数
//                    }
//
//                    // 计算延迟时间 (指数退避: 2, 4, 8秒)
//                    long delaySec = (long) Math.pow(2, currentRetry);
//                    Logger.warn("第" + currentRetry + "次重试，等待" + delaySec + "秒");
//
//                    return Observable.timer(delaySec, TimeUnit.SECONDS);
//                });
//    }

//    public synchronized void stopPolling() {
//        disposables.clear();
//        isPolling = false;
//    }

}
