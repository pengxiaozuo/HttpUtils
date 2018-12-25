package com.peng.httputils.interceptor

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 如果请求max-age>0则更改响应的缓存,缓存数据
 */
internal class ResponseCacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val originalResponse = chain.proceed(chain.request())
        val requestCacheControl = originalRequest.cacheControl()

        if (!requestCacheControl.noCache()
                && originalRequest.cacheControl().maxAgeSeconds() > 0) {

            val responseCacheControl = CacheControl.parse(originalResponse.headers())
            if (responseCacheControl.maxAgeSeconds() > 0
                    && !responseCacheControl.noCache()) {
                return originalResponse
            }
            return originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=${originalRequest.cacheControl().maxAgeSeconds()}")
                    .build()
        }
        return originalResponse
    }
}