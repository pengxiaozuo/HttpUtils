package com.peng.httputils.interceptor

import android.util.Log
import okhttp3.*
import okhttp3.internal.http.HttpHeaders
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit


/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * [application interceptor][OkHttpClient.interceptors] or as a [ ][OkHttpClient.networkInterceptors].
 *
 * The format of the logs created by
 * this class should not be considered stable and may change slightly between releases. If you need
 * a stable logging format, use your own interceptor.
 */
class HttpLoggingInterceptor @JvmOverloads constructor(private val logger: Logger = Logger.DEFAULT) : Interceptor {

    @Volatile
    private var level = Level.NONE

    enum class Level {

        /**
         * 不打印
         */
        NONE,

        /**
         * 打印基本信息，请求方法请求地址，http协议，请求体大小，响应码，响应地址，响应时间，响应体大小
         */
        BASIC,

        /**
         * 打印基本信息加请求头的信息
         */
        HEADERS,

        /**
         * 打印基本信息加请求体信息
         */
        BODY,

        /**
         * 打印所有信息
         */
        FULL
    }


    interface Logger {
        fun log(message: String)

        companion object {

            val DEFAULT: Logger = object : Logger {
                private val prefix = arrayOf(
                        ". ",
                        " .")
                private var index = 0
                override fun log(message: String) {
                    var msg = message
                    if (!msg.startsWith(TOP_LEFT_CORNER) && !msg.startsWith(BOTTOM_LEFT_CORNER)) {
                        msg = "║ $msg"
                    }
                    index = index xor 1
                    Log.d(prefix[index] + "HttpLog", msg)
                }
            }
        }
    }

    /**
     * Change the level at which this interceptor logs.
     */
    fun setLevel(level: Level?): HttpLoggingInterceptor {
        if (level == null) throw NullPointerException("level == null. Use Level.NONE instead.")
        this.level = level
        return this
    }

    fun getLevel(): Level {
        return level
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logFull = level == Level.FULL
        val logHeaders = logFull || level == Level.HEADERS
        val logBody = logFull || level == Level.BODY

        val logMsg = ArrayList<String>()

        val requestBody = request.body()
        val hasRequestBody = requestBody != null

        val connection = chain.connection()
        val protocol = if (connection != null) connection.protocol() else Protocol.HTTP_1_1
        var requestStartMessage = "--> " + request.method() + ' '.toString() + request.url() + ' '.toString() + protocol
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody!!.contentLength() + "-byte body)"
        }
        logMsg.add(TOP_LEFT_CORNER + LINE + LINE)
        logMsg.add(requestStartMessage)
        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody!!.contentType() != null) {
                    logMsg.add("Content-Type: ${requestBody.contentType()}")
                }
                if (requestBody.contentLength() != -1L) {
                    logMsg.add("Content-Length: ${requestBody.contentLength()}")
                }
            }

            val headers = request.headers()
            var i = 0
            val count = headers.size()
            while (i < count) {
                val name = headers.name(i)
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equals(name, ignoreCase = true) && !"Content-Length".equals(name, ignoreCase = true)) {
                    logMsg.add("$name: ${headers.value(i)}")
                }
                i++
            }
        }


        if (!logBody || !hasRequestBody) {
            logMsg.add("--> END ${request.method()}")
        } else if (bodyEncoded(request.headers())) {
            logMsg.add("--> END ${request.method()} (encoded body omitted)")
        } else {
            val buffer = Buffer()
            requestBody!!.writeTo(buffer)

            var charset: Charset = UTF8
            val contentType = requestBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8)!!
            }

            logMsg.add("")
            if (isPlaintext(buffer)) {
                logMsg.add(buffer.readString(charset))
                logMsg.add("--> END ${request.method()} (encoded body omitted)")
            } else {
                logMsg.add("--> END " + request.method() + " (binary "
                        + requestBody.contentLength() + "-byte body omitted)")
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logMsg.forEach {
                logger.log(it)
            }
            logger.log("<-- HTTP FAILED: $e")
            logger.log(BOTTOM_LEFT_CORNER + LINE + LINE)
            throw e
        }
        logMsg.forEach {
            logger.log(it)
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body()
        val contentLength = responseBody!!.contentLength()
        val bodySize = if (contentLength != -1L) contentLength.toString() + "-byte" else "unknown-length"
        logger.log("<-- " + response.code() + ' '.toString() + response.message() + ' '.toString()
                + response.request().url() + " (" + tookMs + "ms" + (if (!logHeaders)
            ", "
                    + bodySize + " body"
        else
            "") + ')'.toString())

        if (logHeaders) {
            val headers = response.headers()
            var i = 0
            val count = headers.size()
            while (i < count) {
                logger.log(headers.name(i) + ": " + headers.value(i))
                i++
            }
        }


        if (!logBody || !HttpHeaders.hasBody(response)) {
            logger.log("<-- END HTTP")
        } else if (bodyEncoded(response.headers())) {
            logger.log("<-- END HTTP (encoded body omitted)")
        } else {
            val source = responseBody.source()
            source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
            val buffer = source.buffer()

            var charset: Charset? = UTF8
            val contentType = responseBody.contentType()
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8)
                } catch (e: UnsupportedCharsetException) {
                    logger.log("")
                    logger.log("Couldn't decode the response body; charset is likely malformed.")
                    logger.log("<-- END HTTP")
                    logger.log(BOTTOM_LEFT_CORNER + LINE + LINE)
                    return response
                }

            }

            if (!isPlaintext(buffer)) {
                logger.log("")
                logger.log("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)")
                logger.log(BOTTOM_LEFT_CORNER + LINE + LINE)
                return response
            }

            if (contentLength != 0L) {
                logger.log("")
                var content = buffer.clone().readString(charset!!).trim()
                try {
                    if (content.startsWith("{")) {
                        val jsonObject = JSONObject(content)
                        content = jsonObject.toString(2)

                    }
                    if (content.startsWith("[")) {
                        val jsonArray = JSONArray(content)
                        content = jsonArray.toString(2)
                    }
                } catch (e: Exception) {

                }
                val lines = content.split(System.getProperty("line.separator"))
                lines.forEach {
                    logger.log(it)
                }
            }

            logger.log("<-- END HTTP (" + buffer.size() + "-byte body)")
        }
        logger.log(BOTTOM_LEFT_CORNER + LINE + LINE)

        return response
    }


    private fun bodyEncoded(headers: Headers): Boolean {
        val contentEncoding = headers.get("Content-Encoding")
        return contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)
    }

    companion object {
        private val UTF8 = Charset.forName("UTF-8")

        private const val TOP_LEFT_CORNER = "╔"
        private const val BOTTOM_LEFT_CORNER = "╚"
        private const val LINE = "══════════════════════════════════════════"

        /**
         * Returns true if the body in question probably contains human readable text. Uses a small sample
         * of code points to detect unicode control characters commonly used in binary file signatures.
         */
        internal fun isPlaintext(buffer: Buffer): Boolean {
            try {
                val prefix = Buffer()
                val byteCount = if (buffer.size() < 64) buffer.size() else 64
                buffer.copyTo(prefix, 0, byteCount)
                for (i in 0..15) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                return true
            } catch (e: EOFException) {
                return false // Truncated UTF-8 sequence.
            }

        }
    }
}