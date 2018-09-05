package com.peng.httputils

import com.peng.httputils.https.HttpsConfig
import com.peng.httputils.interceptor.HeadersInterceptor
import com.peng.httputils.interceptor.HttpLoggingInterceptor
import okhttp3.Cache
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object HttpUtils {

    private val retrofitMap = HashMap<String, Retrofit>()

    /**
     * 初始化配置,多个url用同一个配置，可多次调用添加不同的配置
     */
    fun init(vararg baseUrls: String, block: Builder.() -> Unit) {
        var retrofit: Retrofit? = null
        baseUrls.forEach {
            if (retrofit == null) {
                Builder(it).run {
                    block()
                    retrofit = build()
                    retrofitMap.put(it, retrofit!!)
                }
            } else {
                retrofitMap[it] = retrofit!!.newBuilder().baseUrl(it).build()
            }
        }
    }

    /**
     * 使用已经初始化的的配置创建服务，如果初始化的baseUrl仅有一个可以不传，否则报错
     */
    fun <T> create(clazz: Class<T>, baseUrl: String? = null): T {
        if (retrofitMap.size == 0) {
            throw IllegalArgumentException("not init")
        }

        if (baseUrl.isNullOrEmpty() && retrofitMap.size > 1) {
            throw IllegalArgumentException("init multiple client, must input baseUrl")
        }


        val retrofit = (if (baseUrl.isNullOrEmpty()) {
            retrofitMap.values.elementAt(0)
        } else {
            retrofitMap[baseUrl]
        }) ?: throw IllegalArgumentException("baseUrl not init")

        return retrofit.create(clazz)
    }

    /**
     * 用一个临时的配置创建服务
     */
    fun <T> create(baseUrl: String, clazz: Class<T>, block: Builder.() -> Unit): T {
        val retrofit = Builder(baseUrl).apply(block).build()
        return retrofit.create(clazz)
    }

    class Builder(private val baseUrl: String) {
        private val okhttpBuilder = OkHttpClient.Builder()
        private val retrofitBuilder = Retrofit.Builder()
        var readTimeout = 10000L
        var connectTimeout = 10000L
        var writeTimeout = 10000L
        var debug = false
        var cache: Cache? = null
        var retry = true
        var cookieJar: CookieJar? = null
        var headers: Map<String, String>? = null
        var httpsConfig: HttpsConfig? = null

        fun addInterceptor(interceptor: Interceptor): Builder {
            okhttpBuilder.addInterceptor(interceptor)
            return this
        }

        fun addNetworkInterceptor(interceptor: Interceptor): Builder {
            okhttpBuilder.addNetworkInterceptor(interceptor)
            return this
        }

        fun addConverterFactory(factory: Converter.Factory): Builder {
            retrofitBuilder.addConverterFactory(factory)
            return this
        }

        fun addCallAdapterFactory(factory: CallAdapter.Factory): Builder {
            retrofitBuilder.addCallAdapterFactory(factory)
            return this
        }

        fun build(): Retrofit {
            okhttpBuilder.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                    .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(retry)
            cache?.let {
                okhttpBuilder.cache(it)
            }
            cookieJar?.let {
                okhttpBuilder.cookieJar(it)
            }

            httpsConfig?.let {
                if (it.sslSocketFactory != null && it.trustManager != null) {
                    okhttpBuilder.sslSocketFactory(it.sslSocketFactory!!, it.trustManager!!)
                }

                if (it.hostnameVerifier != null) {
                    okhttpBuilder.hostnameVerifier(it.hostnameVerifier!!)
                }
            }

            headers?.let {
                val headersInterceptor = HeadersInterceptor(it)
                okhttpBuilder.addInterceptor(headersInterceptor)
            }

            if (debug) {
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.FULL)
                okhttpBuilder.addInterceptor(loggingInterceptor)
            }

            return retrofitBuilder.baseUrl(baseUrl)
                    .client(okhttpBuilder.build())
                    .build()
        }
    }

}