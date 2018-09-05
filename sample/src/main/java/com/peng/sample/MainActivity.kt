package com.peng.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.peng.httputils.HttpUtils
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val service1 = HttpUtils.create(GithubService::class.java, GithubService.baseUrl)
        service1.getUser("pengxiaozuo")
                .subscribeOn(Schedulers.io())
                .subscribe({

                }, {
                    it.printStackTrace()
                })

        val service2 = HttpUtils.create(HttpsService::class.java, HttpsService.baseUrl)
        service2.test().subscribeOn(Schedulers.io())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
    }
}
