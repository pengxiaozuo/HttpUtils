package com.peng.httputils.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * ```
 * 有网的情况默认，没有网络的的情况接受4周内的过期缓存
 * 需要添加权限 android.permission.ACCESS_NETWORK_STATE
 * ```
 */
class NetworkCacheInterceptor(private val context: Context) : Interceptor {

    @SuppressLint("MissingPermission")
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (isAvailable(context.applicationContext)) {
            return chain.proceed(request)
        } else {
            val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
            return chain.proceed(request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                    .build())
        }
    }

    @SuppressLint("MissingPermission")
    fun isAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.activeNetworkInfo
        return info != null && info.isAvailable
    }

}