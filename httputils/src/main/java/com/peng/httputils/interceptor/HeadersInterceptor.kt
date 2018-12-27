package com.peng.httputils.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class HeadersInterceptor(private val map: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originHeaders = chain.request().headers()
        val requestBuilder = chain.request().newBuilder()
        for ((k, v) in map) {
            if (originHeaders[k] != null) {
                requestBuilder.header(k, v)
            }
        }
        return chain.proceed(requestBuilder.build())
    }
}