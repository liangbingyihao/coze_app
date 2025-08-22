package sdk.chat.demo.robot.api;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenRefreshInterceptor implements Interceptor {
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public TokenRefreshInterceptor() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 第一次尝试请求
        Response response = chain.proceed(addAuthHeader(originalRequest));

        // 检查Token是否过期
        if (isTokenExpired(response)) {
            response.close();

            // 同步刷新Token
            synchronized (isRefreshing) {
                if (!isRefreshing.get()) {
                    isRefreshing.set(true);
                    try {
                        String newToken = GWApiManager.shared().refreshTokenSync();
                        if (newToken != null && !newToken.isEmpty()) {
                            // 使用新Token重试请求
                            return chain.proceed(addAuthHeader(originalRequest, newToken));
                        }
                    } catch (Exception e) {
//                        throw NeedLoginException("Token刷新失败，请重新登录");
                    } finally {
                        isRefreshing.set(false);
                    }
                }
            }

            // 刷新失败或正在刷新，返回原始错误
            return chain.proceed(addAuthHeader(originalRequest));
        }

        return response;
    }

    private boolean isTokenExpired(Response response) {
        if (response.code() == 401) {
            try {
                String responseBody = response.peekBody(1024).string();
                return responseBody.toLowerCase().contains("expired") ||
                        responseBody.contains("invalid_token") ||
                        responseBody.contains("Authorization");
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    private Request addAuthHeader(Request originalRequest) {
        return addAuthHeader(originalRequest, GWApiManager.shared().getAccessToken());
    }

    private Request addAuthHeader(Request originalRequest, String token) {
        if (token == null || token.isEmpty()) {
            return originalRequest;
        }
        return originalRequest.newBuilder()
                .header("Authorization", token)
                .build();
    }
}