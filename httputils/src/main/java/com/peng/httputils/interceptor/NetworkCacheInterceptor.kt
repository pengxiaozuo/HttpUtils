package com.peng.httputils.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.support.annotation.RequiresPermission
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * ```
 * 有网的情况 根据headers，没有网络的的情况访问缓存,如果
 * 需要添加权限 android.permission.ACCESS_NETWORK_STATE
 * ```
 */
class NetworkCacheInterceptor(private val context: Context) : Interceptor {

    @SuppressLint("MissingPermission")
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val cacheControl = CacheControl.Builder()
                .maxAge(0, TimeUnit.SECONDS)
                .maxStale(365, TimeUnit.DAYS)
                .build()

        if (!isAvailable(context.applicationContext)) {
            request = request.newBuilder().cacheControl(cacheControl)
                    .build()
        }

        val originalResponse = chain.proceed(request)

        if (isAvailable(context.applicationContext)) {
            val maxAge = 60 // read from cache for 1 minute
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=$maxAge")
                    .build()
        } else {
            val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                    .build()
        }
    }

    @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
    fun isAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.activeNetworkInfo
        return info != null && info.isAvailable
    }

}