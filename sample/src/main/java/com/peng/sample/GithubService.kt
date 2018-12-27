package com.peng.sample

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubService {

    companion object {
        const val baseUrl = "https://api.github.com/"
    }

    @GET("users/{login}")
    fun getUser(@Path("login") login: String): Call<ResponseBody>
}