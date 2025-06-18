package sdk.chat.demo.robot.api;

import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.api.model.ImageDailyList;
import sdk.chat.demo.robot.api.model.ImageItem;
import sdk.chat.demo.robot.api.model.ImageTag;
import sdk.chat.demo.robot.api.model.ImageTagList;
import sdk.chat.demo.robot.extensions.DateLocalizationUtil;
import sdk.guru.common.RX;

public class ImageApi {
    private static ImageTagList imageTagCache;
    private final static Gson gson = new Gson();
    private final static String URL2 = "https://api-test.kolacdn.xyz/api/v1/app/";
    private final static String URL_IMAGE_TAG = URL2 + "scripture/background";
    private final static String URL_IMAGE_DAILY_GW = URL2 + "scripture/daily";
    private final static String KEY_CACHE_IMG_DAILY = "bibleDaily";
    private static String oldestImageDailyDate = null;
//    private final static OkHttpClient client;
//
//    static {
//        client = new OkHttpClient.Builder()
//                .cache(new Cache(new File(MainApp.getContext().getCacheDir(), "okhttp_cache"), 10 * 1024 * 1024L))
//                .addInterceptor(new TokenRefreshInterceptor()) // 应用层拦截器
//                .build();
//    }

    public static Single<ImageTagList> listImageTags() {
        return imageTagCache != null
                ? Single.just(imageTagCache)
                : ImageApi.listImageTags(0, 0)
                .doOnSuccess(tagList -> imageTagCache = tagList);
    }

    public static String getRandomImageByTag(String tag) {
        if (imageTagCache == null) {
            return null;
        }
        Random random = new Random();
        List<ImageTag> tags = imageTagCache.getTags();
        for (ImageTag imageTag : tags) {
            if (imageTag.getName().equals(tag)) {
                List<ImageItem> items = imageTag.getImages();
                ImageItem image = items.get(random.nextInt(items.size()));
                return image.getUrl();
            }
        }

        List<ImageItem> items = tags.get(random.nextInt(tags.size())).getImages();
        return items.get(random.nextInt(items.size())).getUrl();
    }

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

//    public static Single<ImageDailyList> listImageDaily() {
//        return imageDailyList != null
//                ? Single.just(imageDailyList)
//                : ImageApi.listImageDaily()
//                .subscribeOn(RX.io())
//                .doOnSuccess(tagList -> imageDailyList = tagList);
//    }

    /**
     * 获取所有 date < endDate 的数据（保持降序）
     *
     * @param endDate 截止日期（yyyy-MM-dd）(包括endDate)
     * @return 符合条件的子列表
     */
    public static Single<List<ImageDaily>> listImageDaily(String endDate) {
        return Single.create(emitter -> {
            String today = DateLocalizationUtil.INSTANCE.formatDayAgo(0);
            String endDateStr = endDate;
            if ((endDateStr == null || endDateStr.isEmpty())) {
                endDateStr = today;
            }

            if (oldestImageDailyDate != null && endDateStr.compareTo(oldestImageDailyDate) <= 0) {
                emitter.onSuccess(Collections.emptyList());
                return;
            }
            String cachedData = JsonCacheManager.INSTANCE.get(MainApp.getContext(), KEY_CACHE_IMG_DAILY);
            ImageDailyList cachedImage = cachedData != null ? gson.fromJson(cachedData, ImageDailyList.class) : null;

            String startDate = DateLocalizationUtil.INSTANCE.getDateBefore(endDate, 60);
            List<ImageDaily> imageList = null;
            if (cachedImage != null) {
                imageList = cachedImage.getImgs();
                List<ImageDaily> result = filterBeforeDate(imageList, endDateStr);
                if (!result.isEmpty()) {
                    emitter.onSuccess(result);
                    return;
                }
            }


            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(URL_IMAGE_DAILY_GW))
                    .newBuilder()
                    .addQueryParameter("start_date", startDate)
                    .addQueryParameter("end_date", endDateStr)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = GWApiManager.shared().getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new IOException("HTTP error: " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonObject data = gson.fromJson(responseBody, JsonObject.class).getAsJsonObject("data");
                ImageDailyList newImageDailyList = gson.fromJson(data, ImageDailyList.class);
                List<ImageDaily> newList = null;
                if (newImageDailyList != null) {
                    newList = newImageDailyList.getImgs();
                }
                if (newList == null || newList.isEmpty()) {
                    if (imageList != null && !imageList.isEmpty()) {
                        oldestImageDailyDate = imageList.get(imageList.size() - 1).getDate();
                    }
                    emitter.onSuccess(Collections.emptyList());
                    return;
                } else if (imageList == null || imageList.isEmpty()) {
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_IMG_DAILY, data.toString());
                } else {
                    newImageDailyList.setImgs(mergeImageLists(imageList, newList));
                    JsonCacheManager.INSTANCE.save(MainApp.getContext(), KEY_CACHE_IMG_DAILY, gson.toJson(newImageDailyList));
                    newList = filterBeforeDate(newImageDailyList.getImgs(), endDateStr);
                }
                emitter.onSuccess(newList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取所有 date <= endDate 的数据（保持降序）必须包含endDate
     *
     * @param imageList 降序排列的列表
     * @param endDate   截止日期（yyyy-MM-dd）
     * @return 符合条件的子列表
     */
    public static List<ImageDaily> filterBeforeDate(
            List<ImageDaily> imageList,
            String endDate) {

        List<ImageDaily> result = new ArrayList<>();
        boolean hitEnd=false;
        for (ImageDaily image : imageList) {
            int cmp = image.getDate().compareTo(endDate);
            if(cmp>0){
                continue;
            }
            if(cmp==0){
                hitEnd=true;
            } else if (!hitEnd) {
                return result;
            }
            result.add(image);
        }

        return result;
    }

    /**
     * 合并两个 List<ImageDaily>，按 date 降序排列，去重
     *
     * @param imageList 原始列表（优先保留）
     * @param newList   新列表（如果有更新的数据，可以覆盖）
     * @return 合并后的列表（按 date 降序）
     */
    public static List<ImageDaily> mergeImageLists(
            List<ImageDaily> imageList,
            List<ImageDaily> newList) {

        // 1. 自定义降序 Comparator
        Comparator<String> dateDescComparator = new Comparator<String>() {
            @Override
            public int compare(String date1, String date2) {
                return date2.compareTo(date1); // 降序
            }
        };

        // 2. 使用 TreeMap 存储（自动去重 + 降序）
        Map<String, ImageDaily> mergedMap = new TreeMap<>(dateDescComparator);

        // 3. 先添加 newList（如果希望 newList 覆盖 imageList）
        for (ImageDaily image : newList) {
            mergedMap.put(image.getDate(), image);
        }

        // 4. 再添加 imageList（如果有重复 date，会覆盖 newList）
        for (ImageDaily image : imageList) {
            mergedMap.put(image.getDate(), image);
        }

        // 5. 转换为 List 并返回
        return new ArrayList<>(mergedMap.values());
    }
}
