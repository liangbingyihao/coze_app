package sdk.chat.demo.robot.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import okhttp3.*;

public class SelectiveDiskCacheInterceptor implements Interceptor {
    private final Set<Pattern> cachedUrlPatterns = new HashSet<>();

    /**
     * 添加需要缓存的URL正则规则
     */
    public void addCachedUrlPattern(String regex) {
        cachedUrlPatterns.add(Pattern.compile(regex));
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();

        // 1. 检查当前URL是否需要缓存
        if (!shouldCache(url)) {
            // 强制跳过磁盘缓存（直接访问网络）
            Request newRequest = request.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .noStore() // 禁止存储缓存
                            .noCache() // 禁止使用缓存
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

        // 2. 允许OkHttp默认缓存逻辑
        return chain.proceed(request);
    }

    /**
     * 判断URL是否匹配缓存规则
     */
    private boolean shouldCache(String url) {
        for (Pattern pattern : cachedUrlPatterns) {
            if (pattern.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }
}
