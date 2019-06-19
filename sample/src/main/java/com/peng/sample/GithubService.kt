package com.peng.sample

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubService {

    companion object {
        const val baseUrl = "https://api.github.com/"
    }

    @GET("users/{login}")
    fun getUser(@Path("login") login: String): Call<ResponseBody>

    @GET("users/{login}")
    suspend fun getSuspendUser(@Path("login") login: String): User
}

data class User(
        var name: String = "",
        @SerializedName("avatar_url") var avatarUrl: String = ""
)