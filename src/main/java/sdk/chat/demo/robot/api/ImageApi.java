package sdk.chat.demo.robot.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Objects;

import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import sdk.chat.demo.robot.api.model.ImageTagList;

public class ImageApi {
    private final static Gson gson = new Gson();
    private final static String URL2 = "https://api-test.kolacdn.xyz/api/v1/app/";
    private final static String URL_IMAGE_TAG = URL2 + "scripture/background";


    public static Single<ImageTagList> listImageTags(int page, int limit) {
        return Single.create(emitter -> {
            OkHttpClient client = GWApiManager.shared().getClient();
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_IMAGE_TAG))
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
                        ImageTagList tagList = gson.fromJson(data, ImageTagList.class);
                        emitter.onSuccess(tagList); // 请求成功
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        response.close(); // 关闭 Response
                    }
                }
            });
        });
    }

}
