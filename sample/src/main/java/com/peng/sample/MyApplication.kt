package com.peng.sample

import android.app.Application
import com.peng.httputils.HttpUtils
import com.peng.httputils.https.HttpsConfig
import com.peng.httputils.interceptor.NetworkCacheInterceptor
import okhttp3.Cache
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HttpUtils.init(GithubService.baseUrl, HttpsService.baseUrl) {
            debug = BuildConfig.DEBUG
            cache = Cache(File(getExternalFilesDir(null), "http-cache"), 10 * 1024 * 1024)
            addInterceptor(NetworkCacheInterceptor(this@MyApplication))
            addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            addConverterFactory(GsonConverterFactory.create())
            httpsConfig = HttpsConfig.build {
                allAllow = true
            }
        }
    }
}