# HttpUtils
Http封装，提供简介的配置和使用方式，有问题可以留言讨论

## gradle配置

在project的build.gradle文件中配置

```gradle
allprojects {
    repositories {
      	//..
        maven {
            url 'https://dl.bintray.com/pengxiaozuo/maven'
        }
    }
}
```

在module的build.gradle文件中配置

```gradle
    implementation 'com.peng.httputils:httputils:0.1.0'
```

## 在项目中使用

在Application中配置，除了baseUrl之外，其他都是可选配置

```kotlin
//可以配置多个baseUrl用同一份配置，也可以为每个baseUrl配置不同的配置
HttpUtils.init(GithubService.baseUrl, HttpsService.baseUrl) {
         connectTimeout = 10000L//okhttp配置
         writeTimeout = 10000L//okhttp配置
    	 writeTimeout = 10000L//okhttp配置
         retry = true//okhttp配置
         cookieJar: CookieJar? = null//okhttp配置
         headers: Map<String, String>? = null //全局headrs
         debug = BuildConfig.DEBUG//是否打印日志
         cache = Cache(File(getExternalFilesDir(null), "http-cache"), 10 * 1024 * 1024)//设置缓存目录和大小
         addInterceptor(NetworkCacheInterceptor(this@MyApplication))//添加缓存拦截器
         addCallAdapterFactory(RxJava2CallAdapterFactory.create())
         addConverterFactory(GsonConverterFactory.create())
    		//https 配置
            httpsConfig = HttpsConfig.build {
                //允许所有
                allAllow = true
                hostnameVerifier(...)//okhttp 主机验证
                clientCertificate(...)//okhttp 证书配置
                serverCertificate(...)//okhttp 证书配置
         }
}
```

使用方式和retrofit一毛一样

定义api接口

```kotlin
interface GithubService {

    companion object {
        const val baseUrl = "https://api.github.com/"
    }

    @GET("users/{login}")
    @Headers("Cache-Control: no-cache")
    fun getUser(@Path("login") login: String): Observable<ResponseBody>
}

interface HttpsService {
    companion object {
        const val baseUrl = "https://revoked.badssl.com/"
    }

    @GET("/")
    fun test(): Observable<ResponseBody>
}
```

使用

```kotlin
val service1 = HttpUtils.create(GithubService::class.java, GithubService.baseUrl)
 service1.getUser("pengxiaozuo")
                .subscribeOn(Schedulers.io())
                .subscribe({
				//it -> ResponseBody 
                }, {
                    it.printStackTrace()
                })
val service2 = HttpUtils.create(HttpsService::class.java, HttpsService.baseUrl)
        service2.test().subscribeOn(Schedulers.io())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
```

取消权限

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

