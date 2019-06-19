package com.peng.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.peng.httputils.HttpUtils
import com.peng.httputils.https.HttpsConfig
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Thread {
            //使用已经初始化好的Retrofit创建服务
            val githubService = HttpUtils.create(GithubService::class.java, GithubService.baseUrl)
            //使用创建好的服务获取结果
            val response = githubService.getUser("pengxiaozuo").execute()
            val msg = response.body()?.string()
            Log.d("githubService", msg)
        }.start()

        //使用已经初始化好的Retrofit创建服务,如果初始化过多个baseUrl需要指定具体的baseUrl
        val httpsService = HttpUtils.create(HttpsService::class.java, HttpsService.baseUrl)
        httpsService.test().subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d("httpsService", "String Length: ${it.string().length}")
                }, {
                    Log.e("httpsService", "httpsService request error", it)
                })

        //初始化客户端并创建服务，客户端和配置信息并不会保存
        val httpsServiceTemp = HttpUtils.create(HttpsService.baseUrl, HttpsService::class.java) {
            //配置信息
            addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            //https 配置信息
            httpsConfig = HttpsConfig.build {
                //                allAllow = true
            }
        }

        httpsServiceTemp.test()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d("httpsServiceTemp", "String Length: ${it.string().length}")
                }, {
                    it.printStackTrace()
                    Log.e("httpsServiceTemp", "httpsServiceTemp request error", it)
                })

        GlobalScope.launch {

            val user = HttpUtils.create<GithubService>(GithubService.baseUrl)
                    .getSuspendUser("pengxiaozuo")

            Log.d("getSuspendUser", user.toString())
        }
    }

}
