package com.peng.httputils.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class HeadersInterceptor(private val map: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        for ((k, v) in map) {
            requestBuilder.addHeader(k, v)
        }
        return chain.proceed(requestBuilder.build())
    }
}