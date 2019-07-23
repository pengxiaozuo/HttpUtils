package com.peng.httputils

import com.peng.httputils.https.HttpsConfig
import com.peng.httputils.interceptor.HeadersInterceptor
import com.peng.httputils.interceptor.HttpLoggingInterceptor
import com.peng.httputils.interceptor.ResponseCacheInterceptor
import okhttp3.*
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import java.net.Proxy
import java.net.ProxySelector
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


object HttpUtils {

    private val retrofitMap = LinkedHashMap<String, Retrofit>()

    /**
     * 初始化配置,多个url用同一个配置，可传入不同的baseUrl添加不同的配置
     * 保存配置
     */
    @JvmStatic
    @JvmOverloads
    fun init(vararg baseUrls: String, block: Builder.() -> Unit = {}) {
        require(baseUrls.isNotEmpty()) { "init must input baseUrl" }

        var retrofit: Retrofit? = null
        baseUrls.forEach {
            require(it.trim().isNotEmpty()) { "baseUrl cannot be spaces and whitespace characters" }
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

    inline fun <reified T> create(baseUrl: String = ""): T = create(T::class.java, baseUrl)
    inline fun <reified T> create(baseUrl: String, noinline block: Builder.() -> Unit = {}): T = create(baseUrl, T::class.java, block)


    /**
     * 使用已经初始化的的配置创建服务，如果初始化的baseUrl仅有一个可以不传，否则报错
     */
    @JvmStatic
    @JvmOverloads
    fun <T> create(clazz: Class<T>, baseUrl: String = ""): T {
        require(retrofitMap.size > 0) { "not init" }
//        if (retrofitMap.size > 1) {
//            require(baseUrl.trim().isNotEmpty()) { "init is a multiple retrofit client, must input baseUrl" }
//        }

        val retrofit = (if (baseUrl.trim().isEmpty()) {
            retrofitMap.values.elementAt(0)
        } else {
            retrofitMap[baseUrl]
        }) ?: throw IllegalArgumentException("baseUrl not init")

        return retrofit.create(clazz)
    }

    /**
     * 创建服务，不保存配置
     */
    @JvmStatic
    @JvmOverloads
    fun <T> create(baseUrl: String, clazz: Class<T>, block: Builder.() -> Unit = {}): T {
        val retrofit = Builder(baseUrl).apply(block).build()
        return retrofit.create(clazz)
    }

    class Builder(private val baseUrl: String) {
        private val okhttpBuilder = OkHttpClient.Builder()
        private val retrofitBuilder = Retrofit.Builder()
        var readTimeout = 10000L
        var connectTimeout = 10000L
        var writeTimeout = 10000L
        var printLog = false
        var cache: Cache? = null
        var retry = true
        var cookieJar: CookieJar? = null
        var headers: Map<String, String>? = null
        var authenticator: Authenticator? = null
        var proxyAuthenticator: Authenticator? = null
        var httpsConfig: HttpsConfig? = null
        var proxy: Proxy? = null
        var proxySelector: ProxySelector? = null
        private val interceptors = ArrayList<Interceptor>()
        private val networkInterceptors = ArrayList<Interceptor>()
        private val converterFactories = ArrayList<Converter.Factory>()
        private val callAdapterFactories = ArrayList<CallAdapter.Factory>()

        fun addInterceptor(interceptor: Interceptor): Builder {
            interceptors.add(interceptor)
            return this
        }

        fun addNetworkInterceptor(interceptor: Interceptor): Builder {
            networkInterceptors.add(interceptor)
            return this
        }

        fun addConverterFactory(factory: Converter.Factory): Builder {
            converterFactories.add(factory)
            return this
        }

        fun addCallAdapterFactory(factory: CallAdapter.Factory): Builder {
            callAdapterFactories.add(factory)
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
                    okhttpBuilder.sslSocketFactory(it.sslSocketFactory, it.trustManager)
                }

                if (it.hostnameVerifier != null) {
                    okhttpBuilder.hostnameVerifier(it.hostnameVerifier)
                }
                if (it.certificatePinner != null) {
                    okhttpBuilder.certificatePinner(it.certificatePinner)
                }

                if (it.connectionSpecs != null) {
                    okhttpBuilder.connectionSpecs(it.connectionSpecs)
                }
            }

            authenticator?.let {

                okhttpBuilder.authenticator(it)
            }
            proxyAuthenticator?.let {
                okhttpBuilder.proxyAuthenticator(it)
            }

            proxy?.let {
                okhttpBuilder.proxy(it)
            }

            proxySelector?.let {
                okhttpBuilder.proxySelector(it)
            }

            headers?.let {
                val headersInterceptor = HeadersInterceptor(it)
                okhttpBuilder.addInterceptor(headersInterceptor)
            }

            interceptors.forEach {
                okhttpBuilder.addInterceptor(it)
            }

            if (printLog) {
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.FULL)
                okhttpBuilder.addInterceptor(loggingInterceptor)
            }

            okhttpBuilder.addNetworkInterceptor(ResponseCacheInterceptor())
            networkInterceptors.forEach {
                okhttpBuilder.addNetworkInterceptor(it)
            }

            converterFactories.forEach {
                retrofitBuilder.addConverterFactory(it)
            }

            callAdapterFactories.forEach {
                retrofitBuilder.addCallAdapterFactory(it)
            }

            return retrofitBuilder.baseUrl(baseUrl)
                    .client(okhttpBuilder.build())
                    .build()
        }
    }

}