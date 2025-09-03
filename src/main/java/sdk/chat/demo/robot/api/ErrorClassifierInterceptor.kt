package sdk.chat.demo.robot.api
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

// 客户端网络异常
class ClientNetworkException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

// 服务端不可用异常
class ServerUnavailableException(
    val code: Int,
    val response: Response? = null
) : IOException("服务端错误: $code")

// 业务逻辑异常
class BusinessException(
    val code: Int,
    val response: Response? = null,
    val msg:String = "业务错误"
) : IOException("$code:$msg")

// 服务端不可用异常
class HttpException(
    val code: Int,
    val response: Response? = null
) : IOException("http未知错误: $code")

class ErrorClassifierInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                val errorBody = response.peekBody(1024).string()
                var errorMessage:String = errorBody.toString()
                try {
                    val json = JSONObject(errorBody)
                    errorMessage = json.optString("msg", "Unknown error")
                } catch (e: Exception) {
                    println("Raw error response: $errorBody")
                }

                when (response.code) {
                    401 -> {
                        // 不直接抛出异常，而是返回401响应
                        return response
                    }
                    in 400..499 -> throw BusinessException(response.code,response,errorMessage)
                    in 500..599 -> throw ServerUnavailableException(response.code,response)
                    else -> throw HttpException(response.code,response)
                }
            }
            return response
        } catch (e: Exception) {
            throw when (e) {
                is SocketTimeoutException -> ClientNetworkException("请求超时", e)
                is UnknownHostException -> ClientNetworkException("网络不可用", e)
                is ConnectException -> ClientNetworkException("连接失败", e)
                is SSLHandshakeException -> ClientNetworkException("安全连接失败", e)
                else -> e
            }
        }
    }
}