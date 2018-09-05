package com.peng.sample

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GithubService {

    companion object {
        const val baseUrl = "https://api.github.com/"
    }

    @GET("users/{login}")
    @Headers("Cache-Control: no-cache")
    fun getUser(@Path("login") login: String): Observable<ResponseBody>
}