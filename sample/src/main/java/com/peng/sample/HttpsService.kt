package com.peng.sample

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET

interface HttpsService {
    companion object {
        const val baseUrl = "https://revoked.badssl.com/"
    }

    @GET("/")
    fun test(): Observable<ResponseBody>
}